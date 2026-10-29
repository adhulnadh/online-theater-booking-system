-- Sample Seed Data for Testing

-- Insert Admin & Regular Users (Passwords hashed or sample)
INSERT INTO users (name, email, password_hash, role)
VALUES 
('Admin User', 'admin@example.com', '$2a$10$wT28Z.xGk.u8c2.dFw3mleV3Ewz1z7fU7eT9E/sO3U61q2.2P8/2a', 'ADMIN'),
('John Doe', 'john@example.com', '$2a$10$wT28Z.xGk.u8c2.dFw3mleV3Ewz1z7fU7eT9E/sO3U61q2.2P8/2a', 'USER')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Movies
INSERT INTO movies (title, aspect_ratio, duration, poster_url, rating)
VALUES 
('Inception', '2.39:1', 148, 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80', 8.8),
('Interstellar', '1.90:1', 169, 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80', 8.7),
('Oppenheimer', '2.20:1', 180, 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80', 8.9),
('Dune: Part Two', '1.90:1', 166, 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80', 8.6),
('The Dark Knight', '2.39:1', 152, 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80', 9.0),
('Spider-Man: Across the Spider-Verse', '2.39:1', 140, 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80', 8.7)
ON DUPLICATE KEY UPDATE title=VALUES(title), rating=VALUES(rating);

-- Insert Theaters
INSERT INTO theaters (name, location, screen_size_inches, screen_aspect_ratio)
VALUES 
('Grand IMAX Screen 1', 'Downtown Mall, 4th Floor', 840.0, '1.90:1'),
('Cineplex Screen 2', 'City Center, Main St', 600.0, '2.39:1'),
('Starlight Laser Ultra', 'Westside Plaza, Level 2', 920.0, '2.39:1'),
('Omnimax 70mm GT', 'Science Center Blvd', 1050.0, '1.43:1'),
('Metro Cinema Screen 3', 'Uptown Galleria', 520.0, '1.85:1')
ON DUPLICATE KEY UPDATE name=VALUES(name), screen_size_inches=VALUES(screen_size_inches), screen_aspect_ratio=VALUES(screen_aspect_ratio);

-- Insert Shows
INSERT INTO shows (movie_id, theater_id, start_time)
VALUES 
(1, 1, DATE_ADD(NOW(), INTERVAL 2 HOUR)),
(2, 1, DATE_ADD(NOW(), INTERVAL 6 HOUR)),
(3, 2, DATE_ADD(NOW(), INTERVAL 3 HOUR));

-- Insert Sample Seats for Show 1
INSERT INTO seats (show_id, seat_number, status, locked_until, user_id)
VALUES 
(1, 'A1', 'AVAILABLE', NULL, NULL),
(1, 'A2', 'AVAILABLE', NULL, NULL),
(1, 'A3', 'LOCKED', DATE_ADD(NOW(), INTERVAL 10 MINUTE), 2),
(1, 'A4', 'BOOKED', NULL, 2),
(1, 'B1', 'AVAILABLE', NULL, NULL),
(1, 'B2', 'AVAILABLE', NULL, NULL)
ON DUPLICATE KEY UPDATE status=VALUES(status);
