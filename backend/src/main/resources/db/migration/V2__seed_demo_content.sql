INSERT INTO menus (id,name,slug,description,menu_type,is_published,valid_from,valid_until,display_order) VALUES
('10000000-0000-0000-0000-000000000001','Dinner','dinner','A considered expression of the season, served à la carte.','DINNER',TRUE,'2026-09-01','2026-11-30',1),
('10000000-0000-0000-0000-000000000002','Tasting','tasting','A guided progression through the kitchen’s current point of view.','TASTING',TRUE,'2026-09-01','2026-11-30',2),
('10000000-0000-0000-0000-000000000003','Cocktails','cocktails','House cocktails composed with orchard fruit, garden herbs, and quiet restraint.','COCKTAILS',TRUE,NULL,NULL,3),
('10000000-0000-0000-0000-000000000004','Wine','wine','A thoughtful cellar of old-world character and independent makers.','WINE',TRUE,NULL,NULL,4),
('10000000-0000-0000-0000-000000000005','Desserts','desserts','A final, delicate note from the pastry kitchen.','DESSERTS',TRUE,NULL,NULL,5);

INSERT INTO menu_categories (id,menu_id,name,slug,description,display_order) VALUES
('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','To Begin','to-begin','A first taste of the season.',1),
('20000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000001','From the Land & Sea','land-sea','Thoughtful sourcing, simply expressed.',2),
('20000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000002','The Tasting','the-tasting','Seven moments from the autumn table.',1),
('20000000-0000-0000-0000-000000000004','10000000-0000-0000-0000-000000000003','Signatures','signatures','Aperitif, stirred, and spirit-free.',1),
('20000000-0000-0000-0000-000000000005','10000000-0000-0000-0000-000000000004','By the Glass','by-the-glass','A changing selection from our cellar.',1),
('20000000-0000-0000-0000-000000000006','10000000-0000-0000-0000-000000000005','The Last Course','the-last-course','A gentle close to the evening.',1);

INSERT INTO menu_items (id,category_id,name,description,price,image_url,is_vegetarian,is_vegan,is_gluten_free,is_featured,is_available,is_seasonal,display_order) VALUES
('30000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','Citrus-Cured Hamachi','Green apple, finger lime, fennel pollen, and a clear broth of garden herbs.',28,'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=1200&q=85',FALSE,TRUE,TRUE,TRUE,TRUE,TRUE,1),
('30000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000001','Wild Mushroom Agnolotti','Hand-folded pasta, roasted cep, aged parmesan, and sage browned in cultured butter.',32,'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,2),
('30000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000002','Seared Scallop','Sweet corn, smoked mussel, bronze fennel, and a warm shellfish emulsion.',38,'https://images.unsplash.com/photo-1559737558-2f5a35f4523b?auto=format&fit=crop&w=1200&q=85',FALSE,FALSE,TRUE,TRUE,TRUE,TRUE,1),
('30000000-0000-0000-0000-000000000004','20000000-0000-0000-0000-000000000002','Dry-Aged Duck Breast','Thirty-day aged duck, quince, black garlic, and a jus scented with juniper.',56,'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1200&q=85',FALSE,FALSE,TRUE,TRUE,TRUE,TRUE,2),
('30000000-0000-0000-0000-000000000005','20000000-0000-0000-0000-000000000002','Roasted Kabocha','Cultivated grains, charred brassica, and a bright sauce of parsley and lemon.',34,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,3),
('30000000-0000-0000-0000-000000000006','20000000-0000-0000-0000-000000000003','First Light','Delicate oyster, cucumber, and a cool breath of dill.',0,'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=1200&q=85',FALSE,FALSE,TRUE,FALSE,TRUE,TRUE,1),
('30000000-0000-0000-0000-000000000007','20000000-0000-0000-0000-000000000003','Orchard','Apple, cultured cream, buckwheat, and marigold.',0,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,2),
('30000000-0000-0000-0000-000000000008','20000000-0000-0000-0000-000000000003','Tide','Scallop, sweet corn, smoked mussel, and sea herbs.',0,'https://images.unsplash.com/photo-1559737558-2f5a35f4523b?auto=format&fit=crop&w=1200&q=85',FALSE,FALSE,TRUE,FALSE,TRUE,TRUE,3),
('30000000-0000-0000-0000-000000000009','20000000-0000-0000-0000-000000000003','Grove','Woodland mushroom, hand-rolled agnolotti, and sage.',0,'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,4),
('30000000-0000-0000-0000-000000000010','20000000-0000-0000-0000-000000000003','Ember','Aged duck, quince, black garlic, and juniper.',0,'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1200&q=85',FALSE,FALSE,TRUE,FALSE,TRUE,TRUE,5),
('30000000-0000-0000-0000-000000000011','20000000-0000-0000-0000-000000000003','Harvest','Kabocha, cultivated grains, and garden herbs.',0,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,6),
('30000000-0000-0000-0000-000000000012','20000000-0000-0000-0000-000000000003','Afterglow','Dark chocolate, pear, toasted hazelnut, and a last note of coffee.',0,'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,7),
('30000000-0000-0000-0000-000000000013','20000000-0000-0000-0000-000000000004','Pear & Thyme','Pear eau-de-vie, lemon, garden thyme, and a fine sparkling finish.',22,'https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,1),
('30000000-0000-0000-0000-000000000014','20000000-0000-0000-0000-000000000004','The Quiet Hour','A measured pour of rye, walnut, and bittersweet vermouth.',24,'https://images.unsplash.com/photo-1470337458703-46ad1756a187?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,2),
('30000000-0000-0000-0000-000000000015','20000000-0000-0000-0000-000000000004','Garden No. 4','Verjus, bay leaf, and a sparkling infusion of green tea. Alcohol-free.',16,'https://images.unsplash.com/photo-1513558161293-cfaf765edfd7?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,3),
('30000000-0000-0000-0000-000000000016','20000000-0000-0000-0000-000000000005','Domaine de la Verpaille, Mâcon-Villages 2022','Chardonnay with orchard fruit, limestone, and a quiet mineral finish.',24,'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,1),
('30000000-0000-0000-0000-000000000017','20000000-0000-0000-0000-000000000005','Domaine des Roches Neuves, Saumur 2021','Cabernet Franc with red currant, fine herbs, and lifted freshness.',26,'https://images.unsplash.com/photo-1516594915697-87eb3b1c14ea?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,2),
('30000000-0000-0000-0000-000000000018','20000000-0000-0000-0000-000000000005','Champagne Laherte Frères, Ultradition','A grower champagne of orchard fruit, chalk, and fine, persistent bubbles.',32,'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?auto=format&fit=crop&w=1200&q=85',TRUE,TRUE,TRUE,FALSE,TRUE,FALSE,3),
('30000000-0000-0000-0000-000000000019','20000000-0000-0000-0000-000000000006','Chocolate Crémeux','Valrhona chocolate, pear, toasted hazelnut, and a delicate coffee tuile.',18,'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,1),
('30000000-0000-0000-0000-000000000020','20000000-0000-0000-0000-000000000006','Roasted Fig','Late-season fig, sheep’s milk yogurt, honey, and crisp rye.',17,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,2),
('30000000-0000-0000-0000-000000000021','20000000-0000-0000-0000-000000000006','Poached Pear','Pear, vanilla, toasted oat, and a warm infusion of chamomile.',16,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&q=85',TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,3);

INSERT INTO allergens (id,name) VALUES
('40000000-0000-0000-0000-000000000001','Dairy'),
('40000000-0000-0000-0000-000000000002','Gluten'),
('40000000-0000-0000-0000-000000000003','Shellfish'),
('40000000-0000-0000-0000-000000000004','Tree nuts'),
('40000000-0000-0000-0000-000000000005','Egg'),
('40000000-0000-0000-0000-000000000006','Fish');
INSERT INTO menu_item_allergens (menu_item_id,allergen_id) VALUES
('30000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000006'),
('30000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000002'),
('30000000-0000-0000-0000-000000000003','40000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000004','40000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000012','40000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000019','40000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000020','40000000-0000-0000-0000-000000000001');

INSERT INTO dining_experiences (id,name,slug,short_description,full_description,hero_image_url,price,wine_pairing_price,course_count,start_date,end_date,status,is_featured,display_order) VALUES
('50000000-0000-0000-0000-000000000001','Autumn Nocturne','autumn-nocturne','An intimate seven-course exploration of autumn’s most expressive ingredients.','As the light softens, the kitchen turns to orchard fruit, woodland mushrooms, and the warmth of an open flame. Autumn Nocturne is a seven-course conversation between place and season, guided by the small details of each harvest.','https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=2000&q=90',185,95,7,'2026-09-01','2026-11-30','PUBLISHED',TRUE,1),
('50000000-0000-0000-0000-000000000002','The Chef’s Counter','chefs-counter','An intimate chef-guided culinary experience, close to the rhythm of the kitchen.','Settle at the counter for eight courses shaped in real time. Each plate is introduced by the cooks who made it, with thoughtful pairings and a closer view of the craft behind the evening.','https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=2000&q=90',225,115,8,'2026-09-01','2027-03-31','PUBLISHED',FALSE,2),
('50000000-0000-0000-0000-000000000003','Winter Reverie','winter-reverie','A winter tasting experience shaped by quiet warmth and seasonal ingredients.','A six-course pause in the heart of winter: root vegetables, preserved citrus, gentle smoke, and the comforting depth of long-cooked broths.','https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=2000&q=90',165,85,6,'2026-12-01','2027-02-28','PUBLISHED',FALSE,3);
INSERT INTO experience_courses (id,experience_id,course_number,name,description,image_url,display_order) VALUES
('51000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',1,'First Light','Oyster, cucumber, and the cool breath of dill.',NULL,1),
('51000000-0000-0000-0000-000000000002','50000000-0000-0000-0000-000000000001',2,'Orchard','Apple, cultured cream, buckwheat, and marigold.',NULL,2),
('51000000-0000-0000-0000-000000000003','50000000-0000-0000-0000-000000000001',3,'Tide','Sweet scallop, smoked mussel, and sea herbs.',NULL,3),
('51000000-0000-0000-0000-000000000004','50000000-0000-0000-0000-000000000001',4,'Grove','Woodland mushroom agnolotti with sage.',NULL,4),
('51000000-0000-0000-0000-000000000005','50000000-0000-0000-0000-000000000001',5,'Ember','Aged duck, quince, black garlic, and juniper.',NULL,5),
('51000000-0000-0000-0000-000000000006','50000000-0000-0000-0000-000000000001',6,'Harvest','Kabocha, cultivated grains, and garden herbs.',NULL,6),
('51000000-0000-0000-0000-000000000007','50000000-0000-0000-0000-000000000001',7,'Afterglow','Dark chocolate, pear, toasted hazelnut, and coffee.',NULL,7),
('52000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000002',1,'Welcome','A small seasonal bite from the kitchen.',NULL,1),
('52000000-0000-0000-0000-000000000002','50000000-0000-0000-0000-000000000002',2,'Garden','Young vegetables, herbs, and cultured butter.',NULL,2),
('52000000-0000-0000-0000-000000000003','50000000-0000-0000-0000-000000000002',3,'Shell','Scallop, sweet corn, and bronze fennel.',NULL,3),
('52000000-0000-0000-0000-000000000004','50000000-0000-0000-0000-000000000002',4,'Woodland','Wild mushroom, hand-folded pasta, and sage.',NULL,4),
('52000000-0000-0000-0000-000000000005','50000000-0000-0000-0000-000000000002',5,'Ember','Dry-aged duck and the season’s orchard fruit.',NULL,5),
('52000000-0000-0000-0000-000000000006','50000000-0000-0000-0000-000000000002',6,'Interlude','A bright, palate-cleansing note.',NULL,6),
('52000000-0000-0000-0000-000000000007','50000000-0000-0000-0000-000000000002',7,'Sweet','Chocolate, pear, and toasted hazelnut.',NULL,7),
('52000000-0000-0000-0000-000000000008','50000000-0000-0000-0000-000000000002',8,'Parting','A final small confection for the way home.',NULL,8),
('53000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000003',1,'Hearth','A warm welcome of roasted root and herb.',NULL,1),
('53000000-0000-0000-0000-000000000002','50000000-0000-0000-0000-000000000003',2,'Citrus','Preserved citrus, fennel, and winter leaf.',NULL,2),
('53000000-0000-0000-0000-000000000003','50000000-0000-0000-0000-000000000003',3,'Broth','Slow-cooked broth with a bright seasonal finish.',NULL,3),
('53000000-0000-0000-0000-000000000004','50000000-0000-0000-0000-000000000003',4,'Field','Winter squash, grains, and toasted seed.',NULL,4),
('53000000-0000-0000-0000-000000000005','50000000-0000-0000-0000-000000000003',5,'Ember','A gently smoked main course with woodland herbs.',NULL,5),
('53000000-0000-0000-0000-000000000006','50000000-0000-0000-0000-000000000003',6,'Reverie','Warm pear, vanilla, and chamomile.',NULL,6);

INSERT INTO private_dining_spaces (id,name,slug,description,capacity,image_url,display_order,is_active) VALUES
('60000000-0000-0000-0000-000000000001','The Salon','the-salon','A softly lit room for intimate celebrations and unhurried conversation, with a dedicated host and a seasonal menu shaped around the occasion.',14,'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1400&q=85',1,TRUE),
('60000000-0000-0000-0000-000000000002','The Cellar','the-cellar','A considered dining room for longer evenings, generous tables, and menus paired with selections from the Élan cellar.',24,'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1400&q=85',2,TRUE),
('60000000-0000-0000-0000-000000000003','Exclusive Restaurant Hire','exclusive-hire','A full-venue setting for larger celebrations and company gatherings, with space for a tailored dining experience.',70,'https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=1400&q=85',3,TRUE);

INSERT INTO gallery_images (id,title,alt_text,image_url,category,display_order,is_published) VALUES
('70000000-0000-0000-0000-000000000001','The dining room','A warmly lit contemporary dining room with linen-covered tables.','https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1400&q=85','Interiors',1,TRUE),
('70000000-0000-0000-0000-000000000002','A seasonal plate','A refined seasonal dish presented with carefully placed herbs.','https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=85','Cuisine',2,TRUE),
('70000000-0000-0000-0000-000000000003','The kitchen at work','A cook finishing a plate in a softly lit restaurant kitchen.','https://images.unsplash.com/photo-1556911220-e15b29be8c8f?auto=format&fit=crop&w=1400&q=85','Kitchen',3,TRUE),
('70000000-0000-0000-0000-000000000004','Autumn harvest','Fresh seasonal produce arranged on a wooden work surface.','https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=1200&q=85','Ingredients',4,TRUE),
('70000000-0000-0000-0000-000000000005','A quiet pour','A glass of red wine beside a candlelit place setting.','https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?auto=format&fit=crop&w=1200&q=85','Cellar',5,TRUE),
('70000000-0000-0000-0000-000000000006','At the table','An intimate table arranged for an evening service.','https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1400&q=85','Interiors',6,TRUE),
('70000000-0000-0000-0000-000000000007','A handmade pasta','Fresh handmade pasta with herbs and seasonal ingredients.','https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=1200&q=85','Cuisine',7,TRUE),
('70000000-0000-0000-0000-000000000008','A gathering in the Salon','A private dining table set with candles and seasonal flowers.','https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=1400&q=85','Private dining',8,TRUE),
('70000000-0000-0000-0000-000000000009','The finishing touch','A chef carefully garnishing a tasting course.','https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=1400&q=85','Kitchen',9,TRUE),
('70000000-0000-0000-0000-000000000010','A little sweetness','A delicate plated chocolate dessert with seasonal fruit.','https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=1200&q=85','Cuisine',10,TRUE),
('70000000-0000-0000-0000-000000000011','The long table','A long dining table ready for a private gathering.','https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1400&q=85','Private dining',11,TRUE),
('70000000-0000-0000-0000-000000000012','The cellar selection','Bottles from a thoughtful collection of independent wine makers.','https://images.unsplash.com/photo-1516594915697-87eb3b1c14ea?auto=format&fit=crop&w=1200&q=85','Cellar',12,TRUE);

INSERT INTO restaurant_settings (id,restaurant_name,tagline,description,city,contact_email,opening_hours,reservation_policy,dress_code,updated_at) VALUES
('80000000-0000-0000-0000-000000000001','ÉLAN','A seasonal expression of place, craft, and memory.','Contemporary dining shaped by the seasons, thoughtful sourcing, and the artistry of the table.','Northbank, Portland','hello@elan.example','Tuesday–Saturday, 5:30–10:00 pm; Sunday and Monday, closed.','This demonstration does not accept reservations. Online booking is being prepared for a future phase. Please allow 48 hours’ notice for changes when reservations become available.','Evening attire; jackets are welcome, and the room is relaxed in spirit.','2026-10-01 12:00:00');
