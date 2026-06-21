#!/bin/bash

# Fallback icon generation script for AdSense Earnings Tracker.
# Use this only when a real icon set is not already present.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ICON_DIR="$SCRIPT_DIR/icons"

# Create icon directory if it doesn't exist
mkdir -p "$ICON_DIR"

if [ "$1" != "--force" ] \
  && [ -f "$ICON_DIR/icon16.png" ] \
  && [ -f "$ICON_DIR/icon32.png" ] \
  && [ -f "$ICON_DIR/icon48.png" ] \
  && [ -f "$ICON_DIR/icon128.png" ]; then
    echo "Existing PNG icon set found. Use --force to replace it with fallback icons."
    exit 0
fi

# Generate a simple fallback icon in SVG format
cat > "$ICON_DIR/icon.svg" << 'EOF'
<svg width="128" height="128" viewBox="0 0 128 128" xmlns="http://www.w3.org/2000/svg">
  <!-- Background -->
  <rect width="128" height="128" rx="32" fill="#3b82f6"/>
  
  <!-- Dollar sign -->
  <text x="64" y="85" font-size="70" font-weight="bold" text-anchor="middle" fill="white" font-family="Arial">$</text>
  
  <!-- Small indicator circle for updates -->
  <circle cx="100" cy="28" r="20" fill="#10b981" opacity="0.9"/>
  <text x="100" y="38" font-size="20" font-weight="bold" text-anchor="middle" fill="white">+</text>
</svg>
EOF

echo "Icon SVG created at icons/icon.svg"

# Note: Chrome doesn't directly support SVG icons in manifest.json for action icons
# We need PNG versions. You can convert the SVG using:
# - Online tools: https://convertio.co/svg-png/
# - ImageMagick: convert icons/icon.svg icons/icon128.png
# - Inkscape: inkscape icons/icon.svg -w 128 -h 128 -o icons/icon128.png

# For development, we can create minimal PNG files using a data URI approach
# Create a simple Python script to generate PNGs

ICON_DIR="$ICON_DIR" python3 << 'PYTHON'
try:
    from PIL import Image, ImageDraw, ImageFont
    import os
    
    # Create icons directory
    icon_dir = os.environ['ICON_DIR']
    os.makedirs(icon_dir, exist_ok=True)
    
    # Define sizes
    sizes = [16, 32, 48, 128]
    
    for size in sizes:
        # Create image with blue background
        img = Image.new('RGB', (size, size), color=(59, 130, 246))  # Blue #3b82f6
        draw = ImageDraw.Draw(img)
        
        # Try to use a nice font, fallback to default
        try:
            font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", int(size * 0.6))
        except:
            font = ImageFont.load_default()
        
        # Draw dollar sign
        draw.text(
            (size//2, size//2 - int(size*0.1)),
            "$",
            fill=(255, 255, 255),
            font=font,
            anchor="mm"
        )
        
        # Save as PNG
        filepath = os.path.join(icon_dir, f'icon{size}.png')
        img.save(filepath)
        print(f'Created {os.path.relpath(filepath)}')
        
except ImportError:
    print('PIL not installed. Please install Pillow: pip install pillow')
    print('OR manually convert icons/icon.svg to PNG using an online converter')

PYTHON

echo "Icon setup complete."
