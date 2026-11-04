-- Actualización oficial de habitaciones del Hotel Lindomar (Sincelejo, Sucre)
-- Tarifas y comodidades verificadas:
-- Sencilla: $80.000 COP
-- Doble: $100.000 COP
-- Familiar 3 pax: $180.000 COP
-- Familiar 4 pax: $240.000 COP
-- Familiar 6 pax: $360.000 COP

-- 1. Actualización / Inserción de las 7 habitaciones reales
INSERT INTO rooms (number, floor, type, capacity, price, balcony, pet_friendly, services, status, image_url)
VALUES 
  ('101', 1, 'Sencilla', 1, 80000, false, false, 'Wi-Fi · Aire acondicionado · TV · Baño privado', 'AVAILABLE', '/rooms/habitacion-1.jpg'),
  ('102', 1, 'Sencilla', 1, 80000, true, false, 'Wi-Fi · Ventilador · TV · Baño privado', 'AVAILABLE', '/rooms/habitacion-2.jpg'),
  ('201', 2, 'Doble', 2, 100000, false, false, 'Wi-Fi · Aire acondicionado · TV · Baño privado', 'AVAILABLE', '/rooms/habitacion-1.jpg'),
  ('202', 2, 'Doble', 2, 100000, true, false, 'Wi-Fi · Ventilador · TV · Baño privado', 'AVAILABLE', '/hotel/lobby.jpg'),
  ('203', 2, 'Familiar', 3, 180000, false, false, 'Wi-Fi · Aire acondicionado · TV · Baño privado', 'AVAILABLE', '/hotel/sala.jpg'),
  ('301', 3, 'Familiar', 4, 240000, true, false, 'Wi-Fi · Aire acondicionado · TV · Baño privado', 'AVAILABLE', '/hotel/recepcion.jpg'),
  ('302', 3, 'Familiar', 6, 360000, true, false, 'Wi-Fi · Aire acondicionado · TV · Baño privado', 'AVAILABLE', '/rooms/habitacion-2.jpg')
ON CONFLICT (number) DO UPDATE SET
  floor = EXCLUDED.floor,
  type = EXCLUDED.type,
  capacity = EXCLUDED.capacity,
  price = EXCLUDED.price,
  balcony = EXCLUDED.balcony,
  pet_friendly = EXCLUDED.pet_friendly,
  services = EXCLUDED.services,
  image_url = EXCLUDED.image_url;

-- Asegurar actualización explícita si la tabla ya contenía estas habitaciones
UPDATE rooms SET type = 'Sencilla', capacity = 1, price = 80000, services = 'Wi-Fi · Aire acondicionado · TV · Baño privado', image_url = '/rooms/habitacion-1.jpg' WHERE number = '101';
UPDATE rooms SET type = 'Sencilla', capacity = 1, price = 80000, services = 'Wi-Fi · Ventilador · TV · Baño privado', image_url = '/rooms/habitacion-2.jpg' WHERE number = '102';
UPDATE rooms SET type = 'Doble', capacity = 2, price = 100000, services = 'Wi-Fi · Aire acondicionado · TV · Baño privado', image_url = '/rooms/habitacion-1.jpg' WHERE number = '201';
UPDATE rooms SET type = 'Doble', capacity = 2, price = 100000, services = 'Wi-Fi · Ventilador · TV · Baño privado', image_url = '/hotel/lobby.jpg' WHERE number = '202';
UPDATE rooms SET type = 'Familiar', capacity = 3, price = 180000, services = 'Wi-Fi · Aire acondicionado · TV · Baño privado', image_url = '/hotel/sala.jpg' WHERE number = '203';
UPDATE rooms SET type = 'Familiar', capacity = 4, price = 240000, services = 'Wi-Fi · Aire acondicionado · TV · Baño privado', image_url = '/hotel/recepcion.jpg' WHERE number = '301';
UPDATE rooms SET type = 'Familiar', capacity = 6, price = 360000, services = 'Wi-Fi · Aire acondicionado · TV · Baño privado', image_url = '/rooms/habitacion-2.jpg' WHERE number = '302';

-- 2. Limpieza de habitaciones obsoletas (ej. 103 antigua o tipos obsoletos como Deluxe/Suite)
DELETE FROM rooms 
WHERE number NOT IN ('101', '102', '201', '202', '203', '301', '302')
  AND id NOT IN (SELECT room_id FROM reservations WHERE room_id IS NOT NULL);

UPDATE rooms 
SET status = 'OUT_OF_SERVICE' 
WHERE number NOT IN ('101', '102', '201', '202', '203', '301', '302');
