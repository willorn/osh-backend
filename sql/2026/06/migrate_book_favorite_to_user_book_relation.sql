-- 将历史电子书收藏从 osh_fava 迁移到 osh_user_book_relation。
-- 课程及其他资源仍保留在 osh_fava，不受本次迁移影响。

UPDATE osh_user_book_relation relation_record
INNER JOIN osh_fava favorite_record
    ON favorite_record.user_id = relation_record.user_id
    AND favorite_record.goods_id = relation_record.book_id
    AND favorite_record.type = 'book'
SET relation_record.favorited = 1,
    relation_record.favorite_time = COALESCE(relation_record.favorite_time, favorite_record.create_time),
    relation_record.update_time = NOW()
WHERE relation_record.deleted = 0;

INSERT INTO osh_user_book_relation (
    user_id,
    book_id,
    favorited,
    followed,
    purchased,
    favorite_time,
    create_time,
    update_time,
    deleted
)
SELECT
    favorite_record.user_id,
    favorite_record.goods_id,
    1,
    0,
    0,
    favorite_record.create_time,
    NOW(),
    NOW(),
    0
FROM osh_fava favorite_record
WHERE favorite_record.type = 'book'
  AND NOT EXISTS (
      SELECT 1
      FROM osh_user_book_relation relation_record
      WHERE relation_record.user_id = favorite_record.user_id
        AND relation_record.book_id = favorite_record.goods_id
        AND relation_record.deleted = 0
  );
