import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;


public class main {
    public static void main(String[] args) {


        Properties property = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.property");
            property.load(fis);

            String url = property.getProperty("url");
            String path = property.getProperty("path");
            int limit = Integer.parseInt(property.getProperty("limit"));

            System.out.println("Добро пожаловать!");
            System.out.println("Выбранная вами ссылка: " + url);
            System.out.println("Путь куда будут сохраняться картинки: " + path);
            System.out.println("Ограничение по весу: " + limit + "Kb \n");

            page fPage= new page(limit);

            ArrayList<String> urls = fPage.getUrls(url);
            System.out.println("Найдено ссылок: " + urls.size());

            ArrayList<String> imgs = fPage.getImages(urls);
            System.out.println("Всего найдено картинок: " + imgs.size());

            int count = 0;
            int skip = 0;
            int fail = 0;
            for(String item: imgs) {
                count++;
                String status = "";
                switch (fPage.saveImage(item, path)){
                    case 1:
                        status = " Загружен   : ";
                        break;
                    case 2:
                        fail++;
                        status = " Незагружен : ";
                        break;
                    case 0:
                        skip++;
                        status = " Пропущен   : ";
                        break;
                }
                System.out.print(count + "/" + imgs.size() + "("+ (int)((float)count/imgs.size()*100) +"%) : ");
                System.out.print(status);
                System.out.println(item);


            }

            System.out.println("\nВсего загружено: " + (imgs.size() - fail - skip));
            System.out.println("Пропущенно из-за маленького веса: " + skip);
            System.out.println("Не загруженно по иным приичинам: " + fail);


        } catch (FileNotFoundException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }
    }
}
