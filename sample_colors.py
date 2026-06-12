import numpy as np
from PIL import Image

src = r"C:\Users\Никита\Desktop\работа\скрины Стёпы\1.1\8.png"
img = Image.open(src).convert('RGB')
arr = np.array(img)
H, W = arr.shape[:2]
print(f"Image: {W}x{H}")

# Sample colors at various points
points = [
    ("top-left bg", 5, 5),
    ("center bg", H // 2, W // 2),
    ("top center", 30, W // 2),
    ("left card center", H // 2, W // 4),
    ("right card center", H // 2, 3 * W // 4),
    ("between cards", H // 2, W // 2),
    ("left card top", 80, W // 4),
    ("right card top", 80, 3 * W // 4),
]
for name, y, x in points:
    if 0 <= y < H and 0 <= x < W:
        c = arr[y, x]
        print(f"  {name} ({x},{y}): RGB={tuple(int(v) for v in c)}")

# Get unique colors histogram in a sample region of left card
print("\nDominant colors histogram:")
small = arr[::4, ::4].reshape(-1, 3)
# round to nearest 16
qsmall = (small // 16) * 16
unique, counts = np.unique(qsmall, axis=0, return_counts=True)
order = np.argsort(counts)[::-1]
for k in order[:15]:
    print(f"  {tuple(int(v) for v in unique[k])}: {counts[k]}")
