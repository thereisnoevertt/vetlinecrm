import numpy as np
from PIL import Image

src = r"C:\Users\Никита\Desktop\работа\скрины Стёпы\1.1\8_backup.png"
img = Image.open(src).convert('RGB')
arr = np.array(img)

box = (55, 46, 619, 606)
x0, y0, x1, y1 = box
sub = arr[y0:y1, x0:x1]
sub_r = sub[:, :, 0].astype(int)
sub_g = sub[:, :, 1].astype(int)
sub_b = sub[:, :, 2].astype(int)
sub_min = np.minimum(np.minimum(sub_r, sub_g), sub_b)
sub_max = np.maximum(np.maximum(sub_r, sub_g), sub_b)
is_content = (sub_min < 200) | ((sub_max - sub_min) > 50)

card_h, card_w = sub.shape[:2]

# Per-row content counts
row_counts = is_content.sum(axis=1)
print("Row counts top 30:")
for y in range(30):
    if row_counts[y] > 0:
        # Find content x positions in this row
        xs = np.where(is_content[y])[0]
        print(f"  y={y}: count={row_counts[y]}, xs={xs[:10].tolist()}, sample colors:")
        for xi in xs[:5]:
            print(f"    x={xi}: RGB={tuple(int(v) for v in sub[y, xi])}")

print(f"\nTotal rows with content > 5: {(row_counts > 5).sum()}")
