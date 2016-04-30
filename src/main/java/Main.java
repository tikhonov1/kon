import org.jaitools.tiledimage.DiskMemImage;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Main {
    final static int SUB_IMAGE_SIZE = 256; //size of the sub images
    final static int BIG_BLOCK_SIZE = 20; //

    final static int SUB_SUB_IMAGE_SIZE = 50;
    final static int SUB_BLOCK_SIZE = 10;

    public static void main(String[] args) throws IOException {
        long time = System.currentTimeMillis();

        BufferedImage bigImage = ImageIO.read(new File(args[0]));

        // Sample  model specifying the size of image tiles
        int tileW = 256;
        SampleModel sm = ColorModel.getRGBdefault().createCompatibleSampleModel(tileW, tileW);

        DiskMemImage result = new DiskMemImage((SUB_IMAGE_SIZE) * (bigImage.getWidth() / BIG_BLOCK_SIZE) * (SUB_SUB_IMAGE_SIZE / SUB_BLOCK_SIZE),
                (SUB_IMAGE_SIZE) * (bigImage.getHeight() / BIG_BLOCK_SIZE) * (SUB_SUB_IMAGE_SIZE / SUB_BLOCK_SIZE), sm, ColorModel.getRGBdefault());

        Graphics g = result.createGraphics();
        List<ImageEntity> subImages = imageEntitiesForPath(args[1]);
        List<ImageEntity> subSubImages = imageEntitiesForPath(args[2]);


        ConcurrentHashMap<ImageEntity, ImageEntity[][]> map = new ConcurrentHashMap<>();

        ImageEntity[][] imageOfSubImages = new ImageEntity(bigImage).makeSubImagesArr(subImages, BIG_BLOCK_SIZE);

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService execService = Executors.newFixedThreadPool(cores);
        Object lock=new Object();

        for (int i = 0; i < cores; i++) {
            int[] iArr = {i};
            execService.execute(() -> {
                for (int x = (bigImage.getWidth() / BIG_BLOCK_SIZE /*+ 1*/) / cores * iArr[0];
                     x < (bigImage.getWidth() / BIG_BLOCK_SIZE /*+ 1*/) / cores * (iArr[0] + 1); x++) {

                    for (int y = 0; y < (bigImage.getHeight() / BIG_BLOCK_SIZE /*+ 1*/); y++) {
                        System.out.println("x=" + x + ", y=" + y);
                        ImageEntity[][] imageOfSubSubImages;
                        if (map.get(imageOfSubImages[x][y]) == null) {
                            imageOfSubSubImages = imageOfSubImages[x][y].makeSubImagesArr(subSubImages, SUB_BLOCK_SIZE);
                            map.put(imageOfSubImages[x][y], imageOfSubSubImages);
                        } else {
                            imageOfSubSubImages = map.get(imageOfSubImages[x][y]);
                        }

                        for (int x1 = 0; x1 < (imageOfSubImages[x][y].getImage().getWidth() / SUB_BLOCK_SIZE/* + 1*/); x1++) {
                            for (int y1 = 0; y1 < (imageOfSubImages[x][y].getImage().getHeight() / SUB_BLOCK_SIZE /*+ 1*/); y1++) {
                                try {
                                    synchronized (lock) {
                                        g.drawImage(imageOfSubSubImages[x1][y1].getImage(),
                                                x * SUB_IMAGE_SIZE * (SUB_SUB_IMAGE_SIZE / SUB_BLOCK_SIZE) + x1 * SUB_SUB_IMAGE_SIZE,
                                                y * SUB_IMAGE_SIZE * (SUB_SUB_IMAGE_SIZE / SUB_BLOCK_SIZE) + y1 * SUB_SUB_IMAGE_SIZE, null);
                                    }
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
//                                    ImageIO.write(result, "TIFF", new File("error.tiff"));
                                    return;
                                }
                            }
                        }
                    }
                }
            });
        }
        execService.shutdown();
        try {
            execService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        System.out.println((System.currentTimeMillis() - time) / 1000 + " seconds");
        time = System.currentTimeMillis();

        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi());
        registry.registerServiceProvider(new com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi());

//        System.out.println(ImageIO.write(result, "TIFF", new File("result.tiff")) ? "Result wrote" : "Writing FAILED");
        System.out.println((System.currentTimeMillis() - time) / 1000 + " seconds");
        System.out.println();
    }

    static List<ImageEntity> imageEntitiesForPath(String path) throws IOException {
        List<ImageEntity> list = new ArrayList<>();

        for (File file : new File(path).listFiles(((dir, name) -> name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".png")))) {
            ImageEntity imageEntity = new ImageEntity(ImageIO.read(file));
            System.out.println(file.getName() + "=" + imageEntity.avgColor());
            list.add(imageEntity);
        }
        return list;
    }
}