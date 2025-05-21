-- Passwords are Password<user's letter>123
-- Encrypted using https://bcrypt-generator.com/

INSERT INTO local_user (username, password, email, first_name, last_name, is_email_verified) VALUES
    ('usernameA', '$2a$10$XPef.8lBPHjwo1ytX.QJX.EwlTAR56CNUzMkK/PDItfIGsW82S1im', 'emailA@gmail.com', 'userA first name', 'userA last name', TRUE),
    ('usernameB', '$2a$10$oDCn6d1gQlkfkRxNNGd5.e/9SodcnL8qSwWeE/Bw2sSxENSpzIfLS', 'emailB@gmail.com', 'userB first name', 'userB last name', FALSE),
    ('usernameC', '$2a$10$STbcoOCCTbo/.g1D4VOyaOj6guD1Vp7auC0CdJmcY8ujvPjpmW7ia', 'emailC@gmail.com', 'userC first name', 'userC last name', TRUE);

INSERT INTO address(address_line_1, city, country, local_user_id) VALUES
    ('123 Tester Hill', 'Testerton', 'England', 1),
    ('312 Spring Boot', 'Hibernate', 'England', 3);

INSERT INTO product (name, short_description, long_description, price) VALUES
    ('Product #1', 'Product one short description.', 'This is a very long description of product #1.', 5.50),
    ('Product #2', 'Product two short description.', 'This is a very long description of product #2.', 10.56),
    ('Product #3', 'Product three short description.', 'This is a very long description of product #3.', 2.74),
    ('Product #4', 'Product four short description.', 'This is a very long description of product #4.', 15.69),
    ('Product #5', 'Product five short description.', 'This is a very long description of product #5.', 42.59);

INSERT INTO inventory (product_id, quantity) VALUES
    (1, 5),
    (2, 8),
    (3, 12),
    (4, 73),
    (5, 2);

INSERT INTO local_order (address_id, local_user_id) VALUES
    (1, 1),
    (1, 1),
    (1, 1),
    (2, 3),
    (2, 3);

INSERT INTO local_order_quantities (local_order_id, product_id, quantity) VALUES
    (1, 1, 5),
    (1, 2, 5),
    (2, 3, 5),
    (2, 2, 5),
    (2, 5, 5),
    (3, 3, 5),
    (4, 4, 5),
    (4, 2, 5),
    (5, 3, 5),
    (5, 1, 5);