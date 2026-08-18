-- Seed initial system data for SmashMate

INSERT INTO expense_categories (code, name, description, is_system, is_active, sort_order) VALUES
('COURT_FEE',   'Tien san', 'Chi phi thue san tap', TRUE,  TRUE, 1),
('SHUTTLE_FEE', 'Tien cau', 'Chi phi cau long',     TRUE,  TRUE, 2),
('FOOD',        'An uong',  'Chi phi an uong',      FALSE, TRUE, 3),
('OTHER',       'Khac',     'Chi phi khac',         FALSE, TRUE, 4);

INSERT INTO club_settings (setting_key, setting_value, description) VALUES
('club_name',             'SmashMate', 'Ten CLB hien thi'),
('payment_qr_image_path', NULL,        'Duong dan file anh QR code ngan hang');

-- Seed default ADMIN account (password: Admin@123)
INSERT INTO members (full_name, email, password, role, status, joined_date) VALUES
('Admin', 'admin@smashmate.local',
 '$2a$12$iORDzD8ydwAZqtANV/bWYem5NG5ExiS9nLVf5GEi8x8yxME53rkdW',
 'ADMIN', 'ACTIVE', CURRENT_DATE);
