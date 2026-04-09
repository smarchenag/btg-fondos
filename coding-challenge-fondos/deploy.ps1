# ============================================
# BTG Fondos - Script de despliegue en AWS
# ============================================

param(
    [string]$StackName = "btg-fondos",
    [string]$Region = "us-east-1",
    [string]$RepoName = "btg-fondos",
    [string]$ImageTag = "latest",
    [string]$SenderEmail = "",
    [string]$JwtSecret = "",
    [string]$DocDBPassword = ""
)

$ErrorActionPreference = "Stop"

# Obtener Account ID automáticamente
$AWS_ACCOUNT_ID = (aws sts get-caller-identity --query "Account" --output text)
$ECR_URI = "$AWS_ACCOUNT_ID.dkr.ecr.$Region.amazonaws.com/$RepoName"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " BTG Fondos - Despliegue AWS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cuenta:    $AWS_ACCOUNT_ID"
Write-Host "Region:    $Region"
Write-Host "Stack:     $StackName"
Write-Host "ECR:       $ECR_URI"
Write-Host "========================================`n" -ForegroundColor Cyan

# --- Paso 1: Crear repositorio ECR (si no existe) ---
Write-Host "[1/6] Verificando repositorio ECR..." -ForegroundColor Yellow
$ecrExists = aws ecr describe-repositories --repository-names $RepoName --region $Region 2>$null
if (-not $ecrExists) {
    Write-Host "  Creando repositorio ECR..." -ForegroundColor Gray
    aws ecr create-repository --repository-name $RepoName --region $Region | Out-Null
    Write-Host "  Repositorio creado." -ForegroundColor Green
} else {
    Write-Host "  Repositorio ya existe." -ForegroundColor Green
}

# --- Paso 2: Login en ECR ---
Write-Host "[2/6] Autenticando Docker con ECR..." -ForegroundColor Yellow
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$Region.amazonaws.com"
Write-Host ""

# --- Paso 3: Build de la imagen ---
Write-Host "[3/6] Construyendo imagen Docker..." -ForegroundColor Yellow
docker build -t "${RepoName}:${ImageTag}" .
if ($LASTEXITCODE -ne 0) { throw "Error en docker build" }
Write-Host "  Build completado." -ForegroundColor Green

# --- Paso 4: Tag y Push ---
Write-Host "[4/6] Subiendo imagen a ECR..." -ForegroundColor Yellow
docker tag "${RepoName}:${ImageTag}" "${ECR_URI}:${ImageTag}"
docker push "${ECR_URI}:${ImageTag}"
if ($LASTEXITCODE -ne 0) { throw "Error en docker push" }
Write-Host "  Push completado." -ForegroundColor Green

# --- Paso 5: Crear/Actualizar stack CloudFormation ---
Write-Host "[5/6] Desplegando infraestructura con CloudFormation..." -ForegroundColor Yellow
$stackExists = aws cloudformation describe-stacks --stack-name $StackName --region $Region 2>$null

if ($stackExists) {
    # UPDATE: usar UsePreviousValue para secretos
    Write-Host "  Stack existe, actualizando..." -ForegroundColor Gray
    $cfParams = @(
        "ParameterKey=AppImage,ParameterValue=${ECR_URI}:${ImageTag}",
        "ParameterKey=JwtSecret,UsePreviousValue=true",
        "ParameterKey=DocDBMasterPassword,UsePreviousValue=true"
    )
    if ($SenderEmail) {
        $cfParams += "ParameterKey=SesSenderEmail,ParameterValue=$SenderEmail"
    } else {
        $cfParams += "ParameterKey=SesSenderEmail,UsePreviousValue=true"
    }
    try {
        aws cloudformation update-stack `
            --stack-name $StackName `
            --template-body file://cloudformation.yml `
            --parameters @cfParams `
            --capabilities CAPABILITY_NAMED_IAM `
            --region $Region
        Write-Host "  Esperando actualizacion (~10-15 min)..." -ForegroundColor Gray
        aws cloudformation wait stack-update-complete --stack-name $StackName --region $Region
    } catch {
        if ($_.Exception.Message -match "No updates are to be performed") {
            Write-Host "  Sin cambios en infraestructura. Forzando redespliegue del servicio ECS..." -ForegroundColor Gray
            $clusterName = aws cloudformation describe-stack-resource --stack-name $StackName --logical-resource-id ECSCluster --query "StackResourceDetail.PhysicalResourceId" --output text --region $Region
            $serviceName = aws cloudformation describe-stack-resource --stack-name $StackName --logical-resource-id ECSService --query "StackResourceDetail.PhysicalResourceId" --output text --region $Region
            aws ecs update-service --cluster $clusterName --service $serviceName --force-new-deployment --region $Region | Out-Null
            Write-Host "  Redespliegue ECS iniciado." -ForegroundColor Green
        } else {
            throw $_
        }
    }
} else {
    # CREATE: requiere valores explícitos para secretos
    if (-not $JwtSecret) {
        $JwtSecret = Read-Host "Ingrese JWT Secret (min 32 caracteres)"
    }
    if (-not $DocDBPassword) {
        $DocDBPassword = Read-Host "Ingrese DocumentDB Master Password (min 8 caracteres)"
    }
    $cfParams = @(
        "ParameterKey=AppImage,ParameterValue=${ECR_URI}:${ImageTag}",
        "ParameterKey=JwtSecret,ParameterValue=$JwtSecret",
        "ParameterKey=DocDBMasterPassword,ParameterValue=$DocDBPassword"
    )
    if ($SenderEmail) {
        $cfParams += "ParameterKey=SesSenderEmail,ParameterValue=$SenderEmail"
    }
    Write-Host "  Creando stack nuevo (~15-20 min)..." -ForegroundColor Gray
    aws cloudformation create-stack `
        --stack-name $StackName `
        --template-body file://cloudformation.yml `
        --parameters @cfParams `
        --capabilities CAPABILITY_NAMED_IAM `
        --region $Region
    aws cloudformation wait stack-create-complete --stack-name $StackName --region $Region
}
Write-Host "  Infraestructura lista." -ForegroundColor Green

# --- Paso 6: Mostrar resultado ---
Write-Host "`n[6/6] Obteniendo URL de la API..." -ForegroundColor Yellow
$apiUrl = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --query "Stacks[0].Outputs[?OutputKey=='APIEndpoint'].OutputValue" `
    --output text `
    --region $Region

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " DESPLIEGUE COMPLETADO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "API URL: $apiUrl" -ForegroundColor White
Write-Host "Health:  $apiUrl/health" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Green
