package com.hss01248.compress;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThumbnailUtil {


  public static List<String> videoExt = new ArrayList<>();
    public static   List<String> imageExt = new ArrayList<>();

    public static String cacheRoot = "D:\\imgcache\\";

    static {
        videoExt.add( "mp4");
        videoExt.add("mkv");
        videoExt.add( "avi");
        videoExt.add("mpeg");
        videoExt.add("mpg");
        videoExt.add("vob");
        videoExt.add( "wmv");
        videoExt.add( "asf");
        videoExt.add("rmvb");
        videoExt.add("rm");
        videoExt.add( "mov");
        videoExt.add( "vob");
        videoExt.add("flv");
        videoExt.add("ts");


        imageExt.add( "jpg");
        imageExt.add("jpeg");
        imageExt.add( "webp");//失败,没有对应编码器
        imageExt.add( "gif");
        imageExt.add( "png");
        imageExt.add("avif");
        imageExt.add( "heif");
    }

    public static void generateThumbs(String dirPath){
        if(StrUtil.isEmpty(dirPath)){
            return;
        }
        File dir = new File(dirPath);
        if(!dir.exists()){
            StaticLog.warn("dir not exist: "+ dirPath);
            return;
        }
        if(dir.isFile()){
            StaticLog.warn("is file ,not dir : "+ dirPath);
            return;
        }
        File[] files = dir.listFiles();
        if(files == null || files.length ==0){
            StaticLog.warn("dir is empty : "+ dirPath);
            return;
        }
        List<File> subDirs = new ArrayList<>();
        StaticLog.info("<-----------------开始遍历文件夹:"+ dirPath+",共"+files.length+"个文件");

        int folderCount = 0;
        for (File file : files) {
            if(file.isFile()){
                getThumbsForVideoAndImage(file);
            }else {
                subDirs.add(file);
                folderCount ++;
            }
        }
        StaticLog.info("-------------->遍历文件夹结束:"+ dirPath+",共"+files.length+"个文件,其中文件夹个数:"+folderCount);
        if(!subDirs.isEmpty()){
            for (File subDir : subDirs) {
                generateThumbs(subDir.getAbsolutePath());
            }
        }
    }

    public static void getThumbsForVideoAndImage(File file) {
        String path = file.getAbsolutePath();
        if(!file.exists()){
            StaticLog.warn("file not" +
                    " exist: "+path);
            return;
        }
        if(file.length() < 307200){
            StaticLog.debug("文件小于300k,不压缩 "+path);
            return;
        }

        String name = file.getName();
        if(!name.contains(".") || name.endsWith(".")){
            //无法判断文件类型,则返回原文件
            StaticLog.debug("无法判断文件类型,则返回原文件,不压缩 "+path);
           return;
        }
        File cacheFile = getThumbCacheFile(path);
        if(cacheFile.exists()  && cacheFile.length()>0){
            StaticLog.info("has cacheFile 缩略图: "+cacheFile.exists()+" , "+cacheFile.getAbsolutePath());
            return ;
        }

        boolean success = ThumbnailUtil.doGetThumb(file,cacheFile);
    }

    public static File getThumbCacheFile(String path){
        String md5 = ThumbnailUtil.md5(path);
        String md52 = md5+".jpg";
        String parent = md5.substring(md5.length()-2)+"";
        String parent0 = md5.charAt(md5.length()-3)+"";
        //100w张图片,分成三级缓存目录还是二级缓存目录?

        File cacheDir = new File(cacheRoot +parent0+"/"+parent);
        if(!cacheDir.exists()){
            cacheDir.mkdirs();
        }
        File cacheFile = new File(cacheDir,md52);
        return cacheFile;
    }

    public static boolean doGetThumb(File file, File cacheFile) {
        try {
            cacheFile.createNewFile();
            //如果是视频,则生成视频缩略图
            if(isVideo(file)){
                //视频,则加入列表
                StaticLog.info("视频,则加入列表 "+ file.getAbsolutePath());
                addToVideoThumbQueue(file,cacheFile);
                //return getVideoThumb(file,cacheFile);
                return false;
            }else if(isImage(file)){

                long start = System.currentTimeMillis();
                    Thumbnails.of(file)
                            .size(640, 480)
                            .outputQuality(0.7)
                            .outputFormat("jpg")
                            .useExifOrientation(true)
                            .toFile(cacheFile);
                StaticLog.info("图片生成缩略图完成: 耗时: "+(System.currentTimeMillis() - start)+"ms, "+ file.getAbsolutePath()+" --> "+ cacheFile.getAbsolutePath());

            }else {
                StaticLog.info("类型不是音视频:"+ file.getAbsolutePath());
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    volatile static ExecutorService executorService;
    public static void addToVideoThumbQueue(File file, File cacheFile) {
        addToThumbQueue(new Runnable() {
            @Override
            public void run() {
                if(cacheFile.exists() && cacheFile.length()>0){
                    return;
                }
                getVideoThumb(file,cacheFile);
            }
        });
    }

    public static void addToThumbQueue(Runnable runnable){
        if(executorService == null){
            executorService = Executors.newSingleThreadExecutor();
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
            }
        });
    }

    public static  boolean isImage(File file) {
        String name = file.getName();
        if(!name.contains(".") || name.endsWith(".")){
            return false;
        }
        String suffix = name.substring(name.lastIndexOf(".")+1);
        for (String s : imageExt) {
            if(s.equalsIgnoreCase(suffix)){
                return true;
            }
        }
        return false;
    }

    private static  boolean getVideoThumb(File file, File cacheFile) {
        try {
            StaticLog.info("开始生成视频缩略图"+ file.getAbsolutePath()+"--> "+ cacheFile.getAbsolutePath());
            fetchFrame(file.getAbsolutePath(),cacheFile.getAbsolutePath());
            return true;
        }catch (Throwable throwable){
            StaticLog.error(throwable);
        }
        return false;
    }

    /**
     * 获取指定视频的帧并保存为图片至指定目录
     * @param videofile  源视频文件路径
     * @param framefile  截取帧的图片存放路径
     * @throws Exception
     */
    public static void fetchFrame(String videofile, String framefile)
            throws Exception {
        long start = System.currentTimeMillis();
        File targetFile = new File(framefile);
        FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videofile);
        ff.start();
        int lenght = ff.getLengthInFrames();
        int i = 0;
        Frame f = null;
        int pick = 10;
        if(lenght > 450){
            pick = 450;
        }
        while (i < lenght) {
            // 过滤前5帧，避免出现全黑的图片，依自己情况而定
            f = ff.grabFrame();
            if ((i > pick) && (f.image != null)) {
                //执行截图并放入指定位置
                doExecuteFrame(f, framefile);
                break;
            }
            i++;
        }
      /*  Buffer[] img = f.image;
        int owidth = img.width();
        int oheight = img.height();*/

        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage bi = converter.getBufferedImage(f);
        if(bi == null){
            ff.stop();
            return;
        }


        int owidth =bi.getWidth();
        int oheight = bi.getHeight();

        // 对截取的帧进行等比例缩放
        int width = 800;
        int height = (int) (((double) width / owidth) * oheight);
        BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        bi2.getGraphics().drawImage(bi.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                0, 0, null);
        ImageIO.write(bi2, "jpg", targetFile);
        //ff.flush();
        ff.stop();
        StaticLog.info("cost "+ (System.currentTimeMillis() - start)+"ms");
    }

    /**
     * 截取缩略图
     * @param f Frame
     * @param targerFilePath:封面图片存放路径
     */
    private static void doExecuteFrame(Frame f, String targerFilePath) {
        String imagemat = "jpg";
        if (null == f || null == f.image) {
            return;
        }
        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage bi = converter.getBufferedImage(f);


        File output = new File(targerFilePath);
        try {
            ImageIO.write(bi, imagemat, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static boolean isVideo(File file) {
        String name = file.getName();
        if(!name.contains(".") || name.endsWith(".")){
            return false;
        }
        String suffix = name.substring(name.lastIndexOf(".")+1);
        for (String s : videoExt) {
            if(s.equalsIgnoreCase(suffix)){
                return true;
            }
        }
        return false;
    }

    private static byte[] getByte(File file) throws Exception{
        FileInputStream inputStream = new FileInputStream(file);

        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        inputStream.close();
        return bytes;
    }

    public static String md5(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return str.replaceAll(":","").replaceAll("/","");
        }
    }
}
