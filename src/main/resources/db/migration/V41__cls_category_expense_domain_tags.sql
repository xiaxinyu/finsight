-- Re-map daily_spending defaults to expense domain tags (Dining/Shopping/Transport/...).

UPDATE cls_category SET semantic_tag = 'shopping_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND (
    parent_id = 'SHOPPING'
    OR code LIKE 'SHOP-%'
    OR code IN ('DAILY-03', 'DAILY-04')
    OR name LIKE '%超市%'
    OR name LIKE '%购物%'
    OR name LIKE '%网上%'
    OR name LIKE '%电商%'
    OR name LIKE '%服饰%'
    OR name LIKE '%美妆%'
    OR name LIKE '%母婴%'
    OR name LIKE '%家居%'
    OR name LIKE '%耐用品%'
    OR name LIKE '%日用品%'
    OR name LIKE '%百货%'
  );

UPDATE cls_category SET semantic_tag = 'transport_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND (
    parent_id IN ('TRANSPORT', 'TRAVEL')
    OR code LIKE 'TRANS-%'
    OR code LIKE 'TRAVEL-%'
    OR name LIKE '%交通%'
    OR name LIKE '%地铁%'
    OR name LIKE '%公交%'
    OR name LIKE '%打车%'
    OR name LIKE '%网约车%'
    OR name LIKE '%滴滴%'
    OR name LIKE '%停车%'
    OR name LIKE '%油费%'
    OR name LIKE '%充电%'
    OR name LIKE '%过路%'
    OR name LIKE '%车辆%'
    OR name LIKE '%机票%'
    OR name LIKE '%火车%'
    OR name LIKE '%租车%'
    OR name LIKE '%代驾%'
    OR name LIKE '%保养%'
    OR name LIKE '%洗车%'
  );

UPDATE cls_category SET semantic_tag = 'dining_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND (
    code IN ('DAILY-01', 'DAILY-02')
    OR name LIKE '%餐饮%'
    OR name LIKE '%外卖%'
    OR name LIKE '%堂食%'
    OR name LIKE '%早餐%'
    OR name LIKE '%咖啡%'
    OR name LIKE '%饭店%'
    OR name LIKE '%吃饭%'
    OR name LIKE '%小吃%'
  );

UPDATE cls_category SET semantic_tag = 'entertainment_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND (
    parent_id = 'ENT'
    OR code LIKE 'ENT-%'
    OR name LIKE '%娱乐%'
    OR name LIKE '%旅行%'
    OR name LIKE '%旅游%'
    OR name LIKE '%酒店%'
    OR name LIKE '%景点%'
    OR name LIKE '%门票%'
    OR name LIKE '%电影%'
    OR name LIKE '%演出%'
    OR name LIKE '%游戏%'
    OR name LIKE '%健身%'
    OR name LIKE '%运动%'
  );

UPDATE cls_category SET semantic_tag = 'education_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND (
    (parent_id = 'EDU' AND code <> 'EDU-01')
    OR code = 'EDU-02'
    OR name LIKE '%培训%'
    OR name LIKE '%书籍%'
    OR name LIKE '%资料%'
    OR name LIKE '%课程%'
  );

UPDATE cls_category SET semantic_tag = 'dining_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND parent_id = 'LIVING';

UPDATE cls_category SET semantic_tag = 'transport_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND parent_id IN ('TRANSPORT', 'TRAVEL');

UPDATE cls_category SET semantic_tag = 'shopping_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND parent_id = 'SHOPPING';

UPDATE cls_category SET semantic_tag = 'entertainment_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND parent_id = 'ENT';

UPDATE cls_category SET semantic_tag = 'education_spending'
WHERE coalesce(deleted, 0) = 0
  AND semantic_tag = 'daily_spending'
  AND parent_id = 'EDU';
