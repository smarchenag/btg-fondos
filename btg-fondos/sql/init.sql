-- =============================================================================
-- Base de datos BTG - Parte 2: Creación de tablas y datos de prueba
-- =============================================================================

-- ===================== TABLAS =====================

CREATE TABLE cliente (
    id        NUMERIC PRIMARY KEY,
    nombre    VARCHAR NOT NULL,
    apellidos VARCHAR NOT NULL,
    ciudad    VARCHAR NOT NULL
);

CREATE TABLE sucursal (
    id     NUMERIC PRIMARY KEY,
    nombre VARCHAR NOT NULL,
    ciudad VARCHAR NOT NULL
);

CREATE TABLE producto (
    id           NUMERIC PRIMARY KEY,
    nombre       VARCHAR NOT NULL,
    tipoProducto VARCHAR NOT NULL
);

CREATE TABLE inscripcion (
    idProducto NUMERIC NOT NULL REFERENCES producto(id),
    idCliente  NUMERIC NOT NULL REFERENCES cliente(id),
    PRIMARY KEY (idProducto, idCliente)
);

CREATE TABLE disponibilidad (
    idSucursal NUMERIC NOT NULL REFERENCES sucursal(id),
    idProducto NUMERIC NOT NULL REFERENCES producto(id),
    PRIMARY KEY (idSucursal, idProducto)
);

CREATE TABLE visitan (
    idSucursal  NUMERIC NOT NULL REFERENCES sucursal(id),
    idCliente   NUMERIC NOT NULL REFERENCES cliente(id),
    fechaVisita DATE    NOT NULL,
    PRIMARY KEY (idSucursal, idCliente)
);

-- ===================== DATOS DE PRUEBA =====================

-- Clientes
INSERT INTO cliente (id, nombre, apellidos, ciudad) VALUES
(1, 'Carlos',   'García López',     'Bogotá'),
(2, 'María',    'Rodríguez Pérez',  'Medellín'),
(3, 'Andrés',   'Martínez Ruiz',    'Cali'),
(4, 'Laura',    'Hernández Díaz',   'Bogotá'),
(5, 'Santiago', 'Torres Gómez',     'Barranquilla');

-- Sucursales
INSERT INTO sucursal (id, nombre, ciudad) VALUES
(1, 'Sucursal Centro',  'Bogotá'),
(2, 'Sucursal Norte',   'Bogotá'),
(3, 'Sucursal Poblado', 'Medellín'),
(4, 'Sucursal Sur',     'Cali'),
(5, 'Sucursal Caribe',  'Barranquilla');

-- Productos
INSERT INTO producto (id, nombre, tipoProducto) VALUES
(1, 'Cuenta de Ahorro',   'Ahorro'),
(2, 'CDT 90 días',        'Inversión'),
(3, 'Tarjeta de Crédito', 'Crédito'),
(4, 'Fondo de Inversión', 'Inversión'),
(5, 'Cuenta Corriente',   'Ahorro');

-- Disponibilidad (qué productos ofrece cada sucursal)
-- Producto 1 (Cuenta de Ahorro): disponible en TODAS las sucursales
INSERT INTO disponibilidad (idSucursal, idProducto) VALUES
(1, 1), (2, 1), (3, 1), (4, 1), (5, 1);

-- Producto 2 (CDT 90 días): solo en sucursales 1 y 2 (Bogotá)
INSERT INTO disponibilidad (idSucursal, idProducto) VALUES
(1, 2), (2, 2);

-- Producto 3 (Tarjeta de Crédito): solo en sucursal 3 (Medellín)
INSERT INTO disponibilidad (idSucursal, idProducto) VALUES
(3, 3);

-- Producto 4 (Fondo de Inversión): en sucursales 1, 3 y 5
INSERT INTO disponibilidad (idSucursal, idProducto) VALUES
(1, 4), (3, 4), (5, 4);

-- Producto 5 (Cuenta Corriente): solo en sucursal 4 (Cali)
INSERT INTO disponibilidad (idSucursal, idProducto) VALUES
(4, 5);

-- Visitas de clientes a sucursales
-- Carlos (1): visita sucursales 1 y 2 (ambas de Bogotá)
INSERT INTO visitan (idSucursal, idCliente, fechaVisita) VALUES
(1, 1, '2025-01-15'),
(2, 1, '2025-02-20');

-- María (2): visita solo sucursal 3 (Medellín)
INSERT INTO visitan (idSucursal, idCliente, fechaVisita) VALUES
(3, 2, '2025-01-10');

-- Andrés (3): visita sucursales 1, 3 y 4
INSERT INTO visitan (idSucursal, idCliente, fechaVisita) VALUES
(1, 3, '2025-03-01'),
(3, 3, '2025-03-05'),
(4, 3, '2025-03-10');

-- Laura (4): visita sucursales 1 y 2 (Bogotá)
INSERT INTO visitan (idSucursal, idCliente, fechaVisita) VALUES
(1, 4, '2025-02-01'),
(2, 4, '2025-02-15');

-- Santiago (5): visita sucursal 5 (Barranquilla) y 1 (Bogotá)
INSERT INTO visitan (idSucursal, idCliente, fechaVisita) VALUES
(5, 5, '2025-04-01'),
(1, 5, '2025-04-10');

-- Inscripciones de clientes a productos
-- Carlos (1): inscrito en producto 2 (CDT, solo en suc 1,2) → visita 1,2
INSERT INTO inscripcion (idProducto, idCliente) VALUES (2, 1);

-- María (2): inscrita en producto 3 (Tarjeta, solo en suc 3) → visita 3
INSERT INTO inscripcion (idProducto, idCliente) VALUES (3, 2);

-- Andrés (3): inscrito en producto 4 (Fondo, en suc 1,3,5) → NO visita suc 5
INSERT INTO inscripcion (idProducto, idCliente) VALUES (4, 3);

-- Laura (4): inscrita en producto 2 (CDT, suc 1,2) → visita 1,2
--            e inscrita en producto 1 (Ahorro, TODAS las suc) → NO visita 3,4,5
INSERT INTO inscripcion (idProducto, idCliente) VALUES (2, 4), (1, 4);

-- Santiago (5): inscrito en producto 4 (Fondo, en suc 1,3,5) → NO visita suc 3
INSERT INTO inscripcion (idProducto, idCliente) VALUES (4, 5);
