package com.usb.usblibrary;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.util.Arrays;

public class ImageProUtils {

    private ImageProUtils() {
    }

    /**
     * 设置像素为ARGB，可选伪彩处理
     *
     * @param pixel
     * @param pseudoColor
     */
    public static void setArrayARGB(int[] pixel, Boolean pseudoColor) {
        if (pseudoColor) {
            for (int i = 0; i < pixel.length; ++i) {
                pixel[i] = grayToRGB(pixel[i], 2);
            }
        } else {
            for (int i = 0; i < pixel.length; ++i) {
                pixel[i] = setARGB(pixel[i], pixel[i], pixel[i]);
            }
        }
    }

    public static void getImageArray(short[] imageArray, byte[] byteArray) {
        int low, high;
        for (int i = 0; i < imageArray.length && (i * 2 + 1) < byteArray.length; ++i) {
            low = byteArray[i * 2] & 0xff;
            high = byteArray[i * 2 + 1] & 0xff;
            imageArray[i] = (short) (low + (high << 8));
        }
    }

    public static void getImageArray(int[] imageArray, byte[] byteArray) {
        int low, high;
        for (int i = 0; i < imageArray.length && (i * 2 + 1) < byteArray.length; ++i) {
            low = byteArray[i * 2] & 0xff;
            high = byteArray[i * 2 + 1] & 0xff;
            imageArray[i] = low + (high << 8);
        }
    }

    public static boolean setImageOffset(short[] sourceArray, short[] gainArray, short[] offsetArray) {
        if (sourceArray.length != offsetArray.length) {
            return false;
        }
        for (int i = 0; i < sourceArray.length; ++i) {
            if ((gainArray[i] & 0x8000) == 0x8000) {
                if (i > 0) {
                    sourceArray[i]=  sourceArray[i - 1];
                }
            } else {
                if (gainArray[i] < 0x1000) {
                    gainArray[i] = 0x1000;
                }
                sourceArray[i]=(short) (sourceArray[i] * gainArray[i] / 0x1000 + offsetArray[i]);
            }
        }
        return true;
    }

    public static boolean setImageOffset(int[] imageArray, short[] gainArray, short[] offsetArray) {
        if (imageArray.length != offsetArray.length) {
            return false;
        }
        for (int i = 0; i < imageArray.length; ++i) {
            if ((gainArray[i] & 0x8000) == 0x8000) {
                if (i > 0) {
                    imageArray[i] = imageArray[i - 1];
                }
            } else {
                imageArray[i] = imageArray[i] * gainArray[i] / 0x1000 + offsetArray[i];
            }
        }
        return true;
    }

    public static short setPixelRange(short pixelValue) {
        if (pixelValue > 255) {
            pixelValue = 255;
        } else if (pixelValue < 0) {
            pixelValue = 0;
        } else {
        }
        return pixelValue;
    }

    public static int setPixelRange(int pixelValue) {
        if (pixelValue > 255) {
            pixelValue = 255;
        } else if (pixelValue < 0) {
            pixelValue = 0;
        } else {
        }
        return pixelValue;
    }

    public static void setRGBRange(short[] rgbPixel, final int minVal, final int maxVal) {
        int min = maxVal, max = minVal;
        short index;
        double width;

        for (int p : rgbPixel) {
            if (min > p && p > minVal) {
                min = p;
            }
            if (max < p && p < maxVal) {
                max = p;
            }
        }
//        int window_center =(max+min)/2;
//        int window_width=max-min;
//        min   =   (int)((2*window_center   -   window_width)/2.0   +   0.5);
//        max   =   (int)((2*window_center   +   window_width)/2.0   +   0.5);
        width = (double) (max - min) / 256;
        for (int i = 0; i < rgbPixel.length; ++i) {
            if (rgbPixel[i] <= min) {
                rgbPixel[i] = 0;
                continue;
            }
            index = (short) ((rgbPixel[i] - min) / width);
            rgbPixel[i] = setPixelRange(index);
        }
    }

    public static void setRGBRange(int[] rgbPixel, final int minVal, final int maxVal) {
        int min = maxVal, max = minVal, index;
        double width;

        for (int p : rgbPixel) {
            if (min > p && p > minVal) {
                min = p;
            }
            if (max < p && p < maxVal) {
                max = p;
            }
        }
        width = (double) (max - min) / 256;
        for (int i = 0; i < rgbPixel.length; ++i) {
            if (rgbPixel[i] <= min) {
                rgbPixel[i] = 0;
                continue;
            }
            index = (int) ((rgbPixel[i] - min) / width);
            rgbPixel[i] = setPixelRange(index);
        }
    }

    public static boolean setHist(short[] hist, short[] nHist, float[] pHist, float[] cHist) {
        if (nHist.length != pHist.length || nHist.length != cHist.length || nHist.length != 256) {
            return false;
        }
        for (int i = 0; i < hist.length; ++i) {
            ++nHist[setPixelRange(hist[i])];
        }
        for (int i = 0; i < 256; ++i) {
            pHist[i] = (float) nHist[i] / hist.length;
        }
        for (int i = 0; i < 256; ++i) {
            for (int j = 0; j <= i; ++j) {
                cHist[i] += pHist[j];
            }
        }
        for (int i = 0; i < hist.length; ++i) {
            hist[i] = (short) (255 * cHist[hist[i]]);
        }
        Arrays.fill(nHist, (short) 0);
        Arrays.fill(cHist, 0);
        return true;
    }

    public static boolean setHist(int[] hist, short[] nHist, float[] pHist, float[] cHist) {
        if (nHist.length != pHist.length || nHist.length != cHist.length || nHist.length != 256) {
            return false;
        }
        for (int i = 0; i < hist.length; ++i) {
            ++nHist[setPixelRange(hist[i])];
        }
        for (int i = 0; i < 256; ++i) {
            pHist[i] = (float) nHist[i] / hist.length;
        }
        for (int i = 0; i < 256; ++i) {
            for (int j = 0; j <= i; ++j) {
                cHist[i] += pHist[j];
            }
        }
        for (int i = 0; i < hist.length; ++i) {
            hist[i] = (int) (255 * cHist[hist[i]]);
        }
        Arrays.fill(nHist, (short) 0);
        Arrays.fill(cHist, 0);
        return true;
    }

    public static void setContrast(short[] Pixel, float Contrast, int bright) {
        for (int i = 0; i < Pixel.length; ++i) {
            Pixel[i] = (short) (Pixel[i] * Contrast + bright);
        }
    }

    public static void setContrast(int[] Pixel, float Contrast, int bright) {
        for (int i = 0; i < Pixel.length; ++i) {
            Pixel[i] = (int) (Pixel[i] * Contrast + bright);
        }
    }

    /**
     * 把RGB像素转化成int类型的ARGB
     *
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static int setARGB(int red, int green, int blue) {
        int pixel = 0;
        red &= 0xff;
        green &= 0xff;
        blue &= 0xff;
        pixel |= 0xff << 24;
        pixel |= setPixelRange(red) << 16;
        pixel |= setPixelRange(green) << 8;
        pixel |= setPixelRange(blue);
        return pixel;
    }

    /**
     * 伪彩处理，把灰度像素转化为RGB像素
     *
     * @param gray
     * @return
     */
    public static int grayToRGB(int gray, int index) {
        int red = 0, green = 0, blue = 0;
        gray &= 0xff;
        switch (index) {
            case 0:
                //red
                if (gray < 128) {
                    red = 0;
                } else if (gray < 192) {
                    red = 255 / 64 * (gray - 128);
                } else {
                    red = 255;
                }
                //green
                if (gray < 64) {
                    green = 255 / 64 * gray;
                } else if (gray < 192) {
                    green = 255;
                } else {
                    green = -255 / 63 * (gray - 192) + 255;
                }
                //blue
                if (gray < 64) {
                    blue = 255;
                } else if (gray < 128) {
                    blue = -255 / 63 * (gray - 192) + 255;
                } else {
                    blue = 0;
                }
                break;
            case 1:
                red = Math.abs(0 - gray);
                green = Math.abs(127 - gray);
                blue = Math.abs(255 - gray);
                break;
            case 2:
                //red
                if (gray < 128) {
                    red = 0;
                } else if (gray < 192) {
                    red = 4 * gray - 510;
                } else {
                    red = 255;
                }
                //green
                if (gray < 64) {
                    green = 254 - 4 * gray;
                } else if (gray < 128) {
                    green = 4 * gray - 254;
                } else if (gray < 192) {
                    green = 1022 - 4 * gray;
                } else {
                    green = -255 / 63 * (gray - 192) + 255;
                }
                //blue
                if (gray < 64) {
                    blue = 255;
                } else if (gray < 128) {
                    blue = 510 - 4 * gray;
                } else {
                    blue = 0;
                }
                break;
            case 3:
                if (gray <= 51) {
                    blue = 255;
                    green = gray * 5;
                    red = 0;
                } else if (gray <= 102) {
                    gray -= 51;
                    blue = 255 - gray * 5;
                    green = 255;
                    red = 0;
                } else if (gray <= 153) {
                    gray -= 102;
                    blue = 0;
                    green = 255;
                    red = gray * 5;
                } else if (gray <= 204) {
                    gray -= 153;
                    blue = 0;
                    green = 255 - setPixelRange((int) (gray * 128.0 / 51 + 0.5));
                    red = 255;
                } else if (gray <= 255) {
                    gray -= 204;
                    blue = 0;
                    green = 127 - setPixelRange((int) (gray * 127.0 / 51 + 0.5));
                    red = 255;
                }
                break;
        }
        return setARGB(red, green, blue);
    }

    /**
     * 非均匀矫正，一点校正
     *
     * @param PixelSum
     * @param pixelOffset
     * @param aveCount
     * @return
     */
    public static float NonUniformCorrection(long[] PixelSum, short[] gainArray, short[] pixelOffset, final int aveCount) {
        if (PixelSum.length != pixelOffset.length) {
            return -1;
        }
        long sum = 0;

        for (int i = 0; i < PixelSum.length; ++i) {
            PixelSum[i] /= aveCount;
            sum += PixelSum[i];
            PixelSum[i] = PixelSum[i] * (gainArray[i] & 0x1fff) / 0x1000;
        }
        float average = sum / PixelSum.length;

        for (int i = 0; i < PixelSum.length; ++i) {
            pixelOffset[i] = (short) (average - PixelSum[i]);
        }
        return average;
    }

    /********************************/
//滤波处理
    public static void selectSort(short array[], int lenth) {
        int minIndex;
        for (int i = 0; i < lenth - 1; i++) {
            minIndex = i;
            for (int j = i + 1; j < lenth; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                short temp = array[i];
                array[i] = array[minIndex];
                array[minIndex] = temp;
            }
            if (i >= lenth / 2) {
                return;
            }
        }
    }

    public static void selectSort(int array[], int lenth) {
        int minIndex;
        for (int i = 0; i < lenth - 1; i++) {
            minIndex = i;
            for (int j = i + 1; j < lenth; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int temp = array[i];
                array[i] = array[minIndex];
                array[minIndex] = temp;
            }
            if (i >= lenth / 2) {
                return;
            }
        }
    }

    public static short getMedian(short array[], int len) {
        selectSort(array, len);
        return array[len / 2];
    }

    public int getMedian(int array[], int len) {
        selectSort(array, len);
        return array[len / 2];
    }

    public static boolean getKernel(short src[], int width, int x, int y, short array[], int ksize) {
        if (ksize == 3) {
            array[0] = src[y * width + x - 1];
            array[1] = src[y * width + x];
            array[2] = src[y * width + x + 1];
        } else if (ksize == 5) {
            array[0] = src[(y - 1) * width + x];
            array[1] = src[y * width + x - 1];
            array[2] = src[y * width + x];
            array[3] = src[y * width + x + 1];
            array[4] = src[(y + 1) * width + x];
        } else if (ksize == 9) {
            array[0] = src[(y - 1) * width + x - 1];
            array[1] = src[(y - 1) * width + x];
            array[2] = src[(y - 1) * width + x + 1];
            array[3] = src[y * width + x - 1];
            array[4] = src[y * width + x];
            array[5] = src[y * width + x + 1];
            array[6] = src[(y + 1) * width + x - 1];
            array[7] = src[(y + 1) * width + x];
            array[8] = src[(y + 1) * width + x + 1];
        } else {
            return false;
        }
        return true;
    }

    public static void MedianFlitering(short[] src, short[] dst, int width, int hight, int ksize) {
        short[] kernel = new short[ksize];
        for (int y = 0; y < hight; ++y) {
            for (int x = 0; x < width; ++x) {
                if ((x > 1) && (x + 1 < width) && (y > 1) && (y + 1 < hight)) {
                    getKernel(src, width, x, y, kernel, ksize);
                    dst[y * width + x] = getMedian(kernel, ksize);
                } else {
                    dst[y * width + x] = src[y * width + x];
                }
            }
        }
    }

    public static void generateGaussianTemplate(double template[], int kssize, double sigma) {
        final double PI = 3.1415926;
        final double sigma2 = 2 * sigma * sigma;
        int center = kssize / 2; // 模板的中心位置，也就是坐标的原点
        double x2, y2, sum = 0;

        for (int y = 0; y < kssize; ++y) {
            x2 = Math.pow(y - center, 2);
            for (int x = 0; x < kssize; ++x) {
                y2 = Math.pow(x - center, 2);
                template[y * kssize + x] = Math.exp(-(x2 + y2) / sigma2) / (PI * sigma2);
                sum += template[y * kssize + x];
            }
        }
        //double k = 1 / window[0][0]; // 将左上角的系数归一化为1
        for (int y = 0; y < kssize; ++y) {
            for (int x = 0; x < kssize; ++x) {
                template[y * kssize + x] /= sum;
            }
        }
    }

    public static short getGaussian(short array[], double[] template, int len) {
        double sum = 0;
        for (int i = 0; i < len; ++i) {
            sum += array[i] * template[i];
        }
        return (short) sum;
    }

    public static void GaussianFilter(short[] src, short[] dst, int width, int hight, int ksize, double sigma) {
        double[] template = new double[ksize];
        generateGaussianTemplate(template, ksize / 3, sigma);
        short[] kernel = new short[ksize];
        for (int y = 0; y < hight; ++y) {
            for (int x = 0; x < width; ++x) {
                if ((x > 1) && (y > 1) && (y + 1 < hight) && (x + 1 < width)) {
                    getKernel(src, width, x, y, kernel, ksize);
                    dst[y * width + x] = getGaussian(kernel, template, ksize);
                } else {
                    dst[y * width + x] = src[y * width + x];
                }
            }
        }
    }

//    static double[] colorTemplate = new double[256];
//    static double colorSigma = 0;
//
//    public static void generateColorTemplate(double sigma) {
//        if (colorSigma == 0 || colorSigma == sigma) {
//            return;
//        }
//        colorSigma = sigma;
//        final double sigma2 = -0.5 * sigma * sigma;
//        for (int i = 0; i < 256; ++i) {
//            colorTemplate[i] = Math.exp(i * i * sigma2);
//        }
//    }


    public static void generateSpaceTemplate(double template[], int kssize, double sigma2) {
        int center = kssize / 2; // 模板的中心位置，也就是坐标的原点
        double x2, y2;
        for (int y = 0; y < kssize; ++y) {
            x2 = y - center;
            x2 = x2 * x2;
            for (int x = 0; x < kssize; ++x) {
                y2 = x - center;
                y2 = y2 * y2;
                template[y * kssize + x] = Math.exp(-(x2 + y2) * sigma2);
            }
        }
    }

    public static short getBilateral(short array[], double[] template, int len, double sigma2) {
        final short center=array[len / 2];
        double sum = 0, weightSum = 0;
        double weight;
        int gray;
        for (int i = 0; i < len; ++i) {
            gray = array[i] - center;
            weight = Math.exp(gray * gray * sigma2);
            weight = template[i] * weight;
            sum += array[i] * weight;
            weightSum += weight;
        }
        return (short) (sum / weightSum);
    }

    public static void BilateralFilter(short[] src, short[] dst, int width, int hight, int ksize, double spaceSigma, double colorSigma) {
        final double spaceSigma2 = -0.5 * spaceSigma * spaceSigma;
        final double colorSigma2 = -0.5 * colorSigma * colorSigma;
        double[] spaceTemplate = new double[9];
        short[] kernel = new short[9];

        generateSpaceTemplate(spaceTemplate, ksize / 3, spaceSigma2);

        for (int y = 0; y < hight; ++y) {
            for (int x = 0; x < width; ++x) {
                if ((x > 1) && (y > 1) && (y + 1 < hight) && (x + 1 < width)) {
                    getKernel(src, width, x, y, kernel, ksize);
                    dst[y * width + x] = getBilateral(kernel, spaceTemplate, ksize, colorSigma2);
                } else {
                    dst[y * width + x] = src[y * width + x];
                }
            }
        }
    }

    public static short getAver(short array[], int len) {
        int sum = 0;
        for (int i = 0; i < len; ++i) {
            sum += array[i];
        }
        return setPixelRange((short) (sum / len));
    }

    public static void AverFiltering(short[] src, short[] dst, int width, int hight, int ksize) {
        short[] kernel = new short[ksize];
        for (int y = 0; y < hight; ++y) {
            for (int x = 0; x < width; ++x) {
                if ((x > 1) && (y > 1) && (y + 1 < hight) && (x + 1 < width)) {
                    getKernel(src, width, x, y, kernel, ksize);
                    dst[y * width + x] = getAver(kernel, ksize);
                } else {
                    dst[y * width + x] = src[y * width + x];
                }
            }
        }
    }


    /**
     * 未使用到的函数
     */

    /**
     * 打死点标记
     *
     * @param grayData
     * @param x
     * @param y
     * @param width
     * @param height
     */
    private void doBadPixel(short[] grayData, int x, int y, int width, int height) {
        if (x < width && y < height) {
            grayData[y * width + x] = (short) (grayData[y * width + x] | 0x8000);
        }
    }

    public static Bitmap rotateBimap(float degree, Bitmap srcBitmap) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degree);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()
                , matrix, true);
        return bitmap;
    }
}
