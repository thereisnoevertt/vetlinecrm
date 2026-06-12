import numpy as np
from PIL import Image
import sys

src = r"C:\Users\Никита\Desktop\работа\скрины Стёпы\1.1\8.png"
img = Image.open(src).convert('RGB')
arr = np.array(img)
H, W = arr.shape[:2]
print(f"Image: {W}x{H}")

r = arr[:, :, 0].astype(int)
g = arr[:, :, 1].astype(int)
b = arr[:, :, 2].astype(int)

pink_mask = (r > 250) & (b > 245) & (r - g > 10) & (np.abs(r - b) < 10)
print(f"Pink pixels (raw): {pink_mask.sum()}")


def erode(m, iters=1):
    for _ in range(iters):
        out = m.copy()
        out[1:, :] &= m[:-1, :]
        out[:-1, :] &= m[1:, :]
        out[:, 1:] &= m[:, :-1]
        out[:, :-1] &= m[:, 1:]
        out[0, :] = False
        out[-1, :] = False
        out[:, 0] = False
        out[:, -1] = False
        m = out
    return m


pink_mask_clean = erode(pink_mask, 5)
print(f"Pink pixels (cleaned): {pink_mask_clean.sum()}")

col_sum = pink_mask_clean.sum(axis=0)
threshold_c = max(col_sum.max() * 0.1, 5)
in_card = col_sum > threshold_c

boxes = []
i = 0
while i < len(in_card):
    if in_card[i]:
        j = i
        while j < len(in_card) and in_card[j]:
            j += 1
        if j - i > 50:
            sub = pink_mask[:, i:j]
            row_sum = sub.sum(axis=1)
            rt = max(row_sum.max() * 0.1, 5)
            rows = np.where(row_sum > rt)[0]
            cy0, cy1 = rows.min(), rows.max() + 1
            boxes.append((i, cy0, j, cy1))
        i = j
    else:
        i += 1

print(f"Found {len(boxes)} cards: {boxes}")


def dilate(m, iters):
    for _ in range(iters):
        out = m.copy()
        out[1:] |= m[:-1]
        out[:-1] |= m[1:]
        out[:, 1:] |= m[:, :-1]
        out[:, :-1] |= m[:, 1:]
        m = out
    return m


result_arr = arr.copy()

for box in boxes:
    x0, y0, x1, y1 = box
    card_h = y1 - y0
    card_w = x1 - x0
    sub_pink = pink_mask[y0:y1, x0:x1]
    sub_arr = arr[y0:y1, x0:x1]

    card_area = np.zeros_like(sub_pink)
    for yy in range(card_h):
        row = sub_pink[yy]
        if row.any():
            l = int(np.argmax(row))
            rr = len(row) - 1 - int(np.argmax(row[::-1]))
            card_area[yy, l:rr + 1] = True

    sub_r = sub_arr[:, :, 0].astype(int)
    sub_g = sub_arr[:, :, 1].astype(int)
    sub_b = sub_arr[:, :, 2].astype(int)
    sub_min = np.minimum(np.minimum(sub_r, sub_g), sub_b)
    sub_max = np.maximum(np.maximum(sub_r, sub_g), sub_b)
    is_content_pixel = (sub_min < 200) | ((sub_max - sub_min) > 50)
    content_mask = card_area & is_content_pixel
    if not content_mask.any():
        continue

    content_mask_d = dilate(content_mask, 2) & card_area

    row_counts = content_mask.sum(axis=1)
    col_counts = content_mask.sum(axis=0)
    valid_rows = np.where(row_counts > 5)[0]
    valid_cols = np.where(col_counts > 5)[0]
    if len(valid_rows) == 0 or len(valid_cols) == 0:
        continue
    cy0, cy1 = int(valid_rows.min()), int(valid_rows.max()) + 1
    cx0, cx1 = int(valid_cols.min()), int(valid_cols.max()) + 1
    print(f"Card {box}: content bbox in card = ({cx0},{cy0})-({cx1},{cy1})")

    content_h = cy1 - cy0
    content_w = cx1 - cx0

    new_y = (card_h - content_h) // 2
    new_x = (card_w - content_w) // 2

    pys, pxs = np.where(sub_pink)
    sample_idx = len(pys) // 2
    pink_color = arr[y0 + pys[sample_idx], x0 + pxs[sample_idx]].astype(np.uint8)
    print(f"  Pink sample: {pink_color}")

    margin = 4
    py0 = max(0, cy0 - margin)
    py1 = min(card_h, cy1 + margin)
    px0 = max(0, cx0 - margin)
    px1 = min(card_w, cx1 + margin)

    captured = sub_arr[py0:py1, px0:px1].copy()
    captured_mask = content_mask_d[py0:py1, px0:px1]

    erase_region = result_arr[y0 + py0:y0 + py1, x0 + px0:x0 + px1]
    erase_region[captured_mask] = pink_color

    target_y = y0 + new_y - (cy0 - py0)
    target_x = x0 + new_x - (cx0 - px0)

    h_p, w_p = captured.shape[:2]
    ty0 = max(0, target_y)
    tx0 = max(0, target_x)
    ty1 = min(H, target_y + h_p)
    tx1 = min(W, target_x + w_p)

    sy0 = ty0 - target_y
    sy1 = sy0 + (ty1 - ty0)
    sx0 = tx0 - target_x
    sx1 = sx0 + (tx1 - tx0)

    region = result_arr[ty0:ty1, tx0:tx1]
    cap_region = captured[sy0:sy1, sx0:sx1]
    cap_mask_region = captured_mask[sy0:sy1, sx0:sx1]
    region[cap_mask_region] = cap_region[cap_mask_region]

Image.fromarray(result_arr).save(src)
print(f"Saved: {src}")
