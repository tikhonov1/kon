import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageEntity {

    private BufferedImage image;
    private Color avgColor;

    public ImageEntity(BufferedImage image) {
        this.image = image;
    }

    public Color avgColor() {
        if (avgColor != null)
            return avgColor;

        long r = 0, g = 0, b = 0;
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                Color color = new Color(image.getRGB(i, j));
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
            }
        }
        int size = image.getWidth() * image.getHeight();

        avgColor = new Color((int) r / size, (int) g / size, (int) b / size);
        return avgColor;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        avgColor = null;
        this.image = image;
    }

    public ImageEntity[][] makeSubImagesArr(List<ImageEntity> subImages, int blockSize) {
        Color[][] bigImageColors = new Color[image.getWidth() / blockSize + 1][image.getHeight() / blockSize + 1];
        ImageEntity[][] imageOfSubImages = new ImageEntity[image.getWidth() / blockSize + 1][image.getHeight() / blockSize + 1];

        for (int x = 0; x < (image.getWidth() / blockSize /*+ 1*/); x++) {
            for (int y = 0; y < (image.getHeight() / blockSize /*+ 1*/); y++) {
                BufferedImage im = image.getSubimage(x * blockSize, y * blockSize,
                        Math.min(blockSize, image.getWidth() - x * blockSize),
                        Math.min(blockSize, image.getHeight() - y * blockSize));

                Color blockAvgColor = new ImageEntity(im).avgColor();
                bigImageColors[x][y] = blockAvgColor;

                double minDist = Double.MAX_VALUE;
                ImageEntity minIE = subImages.get(0);
                for (ImageEntity iE : subImages) {
                    double dist = iE.distance(blockAvgColor);
                    if (dist < minDist) {
                        minIE = iE;
                        minDist = dist;
                    }
                }

                imageOfSubImages[x][y] = minIE;
            }
        }

        return imageOfSubImages;
    }

    public double distance(Color that) {
        long rmean = ((long) avgColor().getRed() + (long) that.getRed()) / 2;
        long r = (long) avgColor().getRed() - (long) that.getRed();
        long g = (long) avgColor().getGreen() - (long) that.getGreen();
        long b = (long) avgColor().getBlue() - (long) that.getBlue();
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageEntity that = (ImageEntity) o;

        return image != null ? image.equals(that.image) : that.image == null;

    }

    @Override
    public int hashCode() {
        return image != null ? image.hashCode() : 0;
    }

    //    public double distance(Color that) {
//        long r = (long) avgColor.getRed() - (long) that.getRed();
//        long g = (long) avgColor.getGreen() - (long) that.getGreen();
//        long b = (long) avgColor.getBlue() - (long) that.getBlue();
//        return Math.sqrt(Math.pow(r, 2) + Math.pow(g, 2) + Math.pow(b, 2));
//    }

}