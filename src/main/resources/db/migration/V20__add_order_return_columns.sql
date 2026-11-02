ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN return_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN return_reason VARCHAR(500);

-- Orders already delivered (or further along) before this column existed: best available estimate.
UPDATE orders
SET delivered_at = updated_at
WHERE status IN ('DELIVERED', 'RETURN_REQUESTED', 'RETURNED', 'REFUND_PENDING', 'REFUNDED');
