-- Prueba Técnica BTG Pactual - Parte 2
SELECT DISTINCT c.nombre
FROM cliente c
JOIN inscripcion i ON i.idCliente = c.id
WHERE EXISTS (
    -- Existe al menos un producto inscrito por el cliente
    SELECT 1
    FROM inscripcion i2
    WHERE i2.idCliente = c.id
      -- que esta disponible SOLO en sucursales que el cliente visita
      AND NOT EXISTS (
          SELECT 1
          FROM disponibilidad d
          WHERE d.idProducto = i2.idProducto
            AND d.idSucursal NOT IN (
                SELECT v.idSucursal
                FROM visitan v
                WHERE v.idCliente = c.id
            )
      )
);
