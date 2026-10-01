INSERT INTO sites(name,address,whatsapp,image) VALUES
 ('San Isidro','Calle Chacarilla 590, San Isidro','51940958067','/images/sanisidro.jpg'),
 ('Regatas','Av. Ignacio Seminario 210, Chorrillos','51940958067','/images/chorrillos.jpg');
INSERT INTO physical_fields(site_id,name) VALUES (1,'Cancha principal'),(2,'Cancha principal');
INSERT INTO sectors(field_id,name) VALUES (1,'A'),(1,'B'),(1,'C'),(1,'D'),(2,'A'),(2,'B'),(2,'C'),(2,'D'),(2,'E');
INSERT INTO spaces(field_id,name,modality) VALUES
 (1,'Cancha A',7),(1,'Cancha B',7),(1,'Cancha C',7),(1,'Cancha D',7),
 (1,'Mitad A + B',9),(1,'Mitad C + D',9),(1,'Cancha completa',11),
 (2,'Cancha A',7),(2,'Cancha B',7),(2,'Cancha C',5),(2,'Cancha D',5),(2,'Cancha E',5),
 (2,'Mitad A + B',9),(2,'Mitad C + D + E',9),(2,'Cancha completa',11);
INSERT INTO space_sectors VALUES
 (1,1),(2,2),(3,3),(4,4),(5,1),(5,2),(6,3),(6,4),(7,1),(7,2),(7,3),(7,4),
 (8,5),(9,6),(10,7),(11,8),(12,9),(13,5),(13,6),(14,7),(14,8),(14,9),
 (15,5),(15,6),(15,7),(15,8),(15,9);
INSERT INTO operating_hours(site_id,day_of_week,open_hour,close_hour)
 SELECT 1,d,CASE WHEN d <= 5 THEN 16 ELSE 7 END,23 FROM generate_series(1,7) d;
INSERT INTO operating_hours(site_id,day_of_week,open_hour,close_hour)
 SELECT 2,d,7,23 FROM generate_series(1,7) d;
INSERT INTO rates(space_id,day_of_week,start_hour,end_hour,amount)
 SELECT s.id,d,0,18,CASE s.modality WHEN 5 THEN 70 WHEN 7 THEN 100 WHEN 9 THEN 180 ELSE 320 END
 FROM spaces s CROSS JOIN generate_series(1,7) d;
INSERT INTO rates(space_id,day_of_week,start_hour,end_hour,amount)
 SELECT s.id,d,18,24,CASE s.modality WHEN 5 THEN 90 WHEN 7 THEN 130 WHEN 9 THEN 220 ELSE 400 END
 FROM spaces s CROSS JOIN generate_series(1,7) d;
