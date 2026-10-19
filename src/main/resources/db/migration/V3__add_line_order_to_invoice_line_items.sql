-- Invoice.lineItems is a @OneToMany with @OrderColumn(name = "line_order"), which persists
-- the list's order via a dedicated integer column that V1 never created.
ALTER TABLE invoice_line_items ADD COLUMN line_order INTEGER;