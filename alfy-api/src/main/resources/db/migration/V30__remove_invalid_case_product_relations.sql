DELETE relation
FROM product_case_rel relation
LEFT JOIN product product ON product.id = relation.product_id
    AND product.deleted = 0
WHERE product.id IS NULL;
