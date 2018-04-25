import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class page {
    private String sProtocol = "";
    private String sDomen = "";
    private int limit;

    page(int iLimit) {
        limit = iLimit;
    }

    public ArrayList<String> getUrls(String iUrl) {
        ArrayList<String> urls = new ArrayList<>();
        urls.add(iUrl);

        sProtocol = getProtocol(iUrl);
        sDomen = getDomen(iUrl);

        String page = getPage(iUrl);
        Pattern p = Pattern.compile("href=\"/[^\"]+\"");
        Matcher m = p.matcher(page);

        while(m.find()) {
            urls.add(page.substring(m.start()+6, m.end()-1));
        }

        urls = delDuplicate(urls);
        urls = addFullAdress(urls);

        return urls;
    }

    private String getProtocol(String iUrl)
    {
        String result = "";

        Pattern p = Pattern.compile("^http([s]*)");
        Matcher m = p.matcher(iUrl);

        while(m.find()) {
            result = iUrl.substring(m.start(), m.end());
        }
        return result;
    }

    private String getDomen(String iUrl)
    {
        String result = "";

        Pattern p = Pattern.compile("://([^/]+)/");
        Matcher m = p.matcher(iUrl);

        while(m.find()) {
            result = m.group(1);
        }

        return result;
    }


    // Картинками считаем такие форматы как jpg jpeg png gif svg ico
    public ArrayList<String> getImages(ArrayList<String> urls) {
        ArrayList<String> img = new ArrayList<String>();
        Pattern p = Pattern.compile("(.+(\\.jpg|\\.jpeg|\\.png|\\.gif|\\.svg|\\.ico))");

        for(String u: urls) {
            Matcher m = p.matcher(u);

            boolean imgUrl = false;
            while(m.find()) {
                imgUrl = true;
                img.add(m.group(1));
            }

            if(!imgUrl) {
                String page = getPage(u);
                img.addAll(getSrcImage(page));
                img.addAll(getBackImage(page));
            }
        }

        img = replaceHash(img);
        img = delDuplicate(img);
        img = addFullAdress(img);

        return img;
    }

    private ArrayList<String> getSrcImage(String page) {
        ArrayList<String> list = new ArrayList<>();

        Pattern p = Pattern.compile("<img[^>]+src=\"([^\"}]+)\"");
        Matcher m = p.matcher(page);
        while (m.find()) {
            list.add(m.group(1));
        }

        return list;
    }

    private ArrayList<String> getBackImage(String page) {
        ArrayList<String> list = new ArrayList<>();

        Pattern p = Pattern.compile("background:[^;]+;");
        Pattern p2 = Pattern.compile("url[ ]*[(]([^)]+)[)]");
        Matcher m = p.matcher(page);
        while (m.find()) {
            Matcher m2 = p2.matcher(m.group(0));
            while (m2.find()) {
                list.add(m2.group(1));
            }
        }

        return list;
    }

    private ArrayList<String> addFullAdress(ArrayList<String> urls) {
        for(int i = 0; i < urls.size(); ++i) {
            String u = urls.get(i);
            if(!u.startsWith("http")) {
                if (u.startsWith("//")) {
                    urls.set(i, u.replaceAll("^//", sProtocol + "://"));
                } else if (u.startsWith("/")) {
                    urls.set(i, u.replaceAll("^/", sProtocol + "://" + sDomen + "/"));
                }
            }
        }

        return urls;
    }

    private  ArrayList<String> replaceHash(ArrayList<String> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i).replace("&#47;", "/").replace("\"", ""));
        }

        return list;
    }

    private String getPage(String iUrl) {
        String cPage = "";

        try {
            URL url = new URL(iUrl);
            try {
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
                String string = reader.readLine();
                while (string != null) {
                    cPage += string;
                    string = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
            }

        } catch (MalformedURLException ex) {
        }

        return cPage;
    }

    private ArrayList<String> delDuplicate(ArrayList<String> list)
    {
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> hs = new HashSet<>();
        hs.addAll(list);
        result.addAll(hs);
        return result;
    }

    private int getFileLength(String iUrl) {
        int bytes = 0;

        try {
            URL url = new URL(iUrl);
            try {
                BufferedImage image = ImageIO.read(url);
                DataBuffer buff = image.getRaster().getDataBuffer();
                bytes = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8 / 1024;
            } catch (IOException e) {
            } catch (NullPointerException e) {
            }
        } catch (MalformedURLException ex) {
        }

        return bytes;
    }

    private boolean downLimit(String iUrl)
    {
        return (getFileLength(iUrl) >= limit);
    }

    private String getName(String iUrl) {
        String result = "";
        Pattern p = Pattern.compile("/(([^/]+)(\\.jpg|\\.jpeg|\\.png|\\.gif|\\.svg|\\.ico)+|([^/]+))$");
        Matcher m = p.matcher(iUrl);

        while (m.find()){
            if (m.group(2) == null)
                result = m.group(4);
            else
                result = m.group(2);
        }

        return result;
    }

    private String checkName(String name, String format, String path) {
        String tName = name;
        int i = 0;
        while(i < 10000) {
            File f = new File(path + "/" + tName + "." + format);
            if (f.exists() && !f.isDirectory()) {
                tName = name + i;
            } else {
                break;
            }
            i++;
        }

        return tName;
    }

    private String getFormat(String format) {

        switch (format) {
            case "JPEG":
            case "JPG" :
                format = "jpg";
                break;
            case "PNG" :
                format = "png";
                break;
            case "SVG" :
                format = "svg";
                break;
            case "GIF" :
                format = "gif";
                break;
            case "ICO" :
                format = "ico";
                break;
        }

        return format;
    }

    private boolean downloadImage(String iUrl, String path) {

        try {
            URL url = new URL(iUrl);
            try {
                ImageInputStream iis = ImageIO.createImageInputStream(url.openStream());
                Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);

                String format = "";
                while (imageReaders.hasNext()) {
                    ImageReader reader = imageReaders.next();
                    format = reader.getFormatName();
                }

                format = getFormat(format);

                String name = getName(iUrl);
                name = checkName(name, format, path);

                InputStream image = url.openStream();
                Files.copy(image, Paths.get(path + "/" + name + "." + format));
                return true;
            } catch (IOException e) {
            } catch (NullPointerException e) {
            }
        } catch (MalformedURLException ex) {
        }

        return false;
    }

    public int saveImage(String iUrl, String path) {
        if(downLimit(iUrl)) {
            if (downloadImage(iUrl, path)) {
                return 1;
            } else {
                return 2;
            }
        } else {
            return 0;
        }
    }
}
