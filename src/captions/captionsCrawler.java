package captions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class captionsCrawler {

	captionsCrawler()
	{
		
	}
	
	public static final String GET_URL = "https://www.youtube.com/watch?v=oE-RjpaCHvo&list=PLOGi5-fAu8bFFgJj4LMbnLM9cWP9HSQ4k";

    public static final String POST_URL = "http://mops.twse.com.tw/server-java/FileDownLoad";
    
    private static final String buttonID = "YSC";
    private static Connection.Response response;
    private static String cookieButtonPush;
	private static Document cookieDoc;
	private static List<String> AllVideo = new ArrayList<String>();
	private static List<String> AllTitle = new ArrayList<String>();
	private static Map<String, String> MatchTitle = new HashMap<String, String>();
	private static int countAll = 1;
	private static boolean stopWriteListFlag = false;
		
	//從一個Video連結跳至該頁channel內影片List之連結
	public static String jumpToPlaylist(String theChannelUrl) throws IOException
	{
		Document goLayer1 = Jsoup.connect(theChannelUrl).get();
		Elements layer1 = goLayer1.select("h2[class=branded-page-module-title]").select("a[class=yt-uix-button  shelves-play play-all-icon-btn yt-uix-sessionlink yt-uix-button-default yt-uix-button-size-small yt-uix-button-has-icon no-icon-markup]");
		String layer1Url = layer1.attr("href");
		
		Document goLayer2 = Jsoup.connect("https://www.youtube.com"+layer1Url).get();
		Elements layer2 = goLayer2.select("h3[class=playlist-title]").select("a");
		String layer2Url = layer2.attr("href");
		return "https://www.youtube.com"+layer2Url;
	}
	
	//取得該頁影片List所有Video連結(第二頁以後)
	public static void getVideoPath(int htmlNumber) throws IOException
	{
		File inp = new File("D:\\listFile\\result"+htmlNumber+".html");
    	Document ta1 = Jsoup.parse(inp,"UTF-8");
    	
    	Elements ea1 = ta1.select("a[class*=pl-video-title-link yt-uix-tile-link yt-uix-sessionlink  spf-link ]");
    	
    	for(Element vPath: ea1)
    	{
    		String getHref = vPath.attr("href");
    		String getTitle = vPath.text();
    		AllVideo.add("https://www.youtube.com"+getHref);
    		MatchTitle.put("https://www.youtube.com"+getHref, getTitle);
    		//System.out.println(countAll+" "+getHref);
    		countAll++;
    	}
	}
	
	//取得下個button的路徑(路徑寫於js檔中)
	public static String getButtonPath(int htmlNumber) throws IOException
	{
		File inp = new File("D:\\listFile\\result"+htmlNumber+".html");
    	Document ta1 = Jsoup.parse(inp,"UTF-8");
    	
    	Elements ea1 = ta1.select("button[class*=yt-uix-button yt-uix-button-size-default yt-uix-button-default load-more-button yt-uix-load-more browse-items-load-more-button]");
    	String getpath2[] = null;
    	
    	for(Element button: ea1)
    	{
    		String click = button.attr("data-uix-load-more-href");
    		getpath2 = click.split("="); 
    		System.out.println(" "+getpath2[2]);
    	}
    	System.out.println("D:\\listFile\\result"+htmlNumber+".html size is: "+inp.length());
    	if(inp.length()<1024)
    		return null;
    	else
    		return getpath2[2];
	}
	
	//把按下"載入更多頁面"按鈕後取得的頁面寫入html
	public static void createHtml(String nextPath,int htmlNumber) throws IOException
	{
		URL ajax = new URL("https://www.youtube.com/browse_ajax?action_continuation=1&continuation="+nextPath);
    	URLConnection content = ajax.openConnection();
    	BufferedReader in = new BufferedReader(new InputStreamReader(content.getInputStream()));
    	FileWriter out = new FileWriter("D:\\listFile\\result"+htmlNumber+".html");
    	
    	String inputLine;
    	
    	while((inputLine = in.readLine())!= null)
    	{
    		inputLine = inputLine.replaceAll("\\\\u003c", "<");
    		inputLine = inputLine.replaceAll("\\\\u003e", ">");
    		inputLine = inputLine.replaceAll("\\\\u0026", "&");
    		
    		inputLine = inputLine.replaceAll("\\\\n", "\r\n");
    		inputLine = inputLine.replaceAll("\\\\/", "\\/");
    		inputLine = inputLine.replaceAll("\\\\\"", "\"");
    		inputLine = inputLine.replaceAll("\\/\\/", "\\/");
    		
    		out.write(inputLine);
    		out.flush();
    	}
    	out.close();
    	in.close();
	}
	
	//從影片清單頁開始爬
    public static void VideoList(String theChannelListUrl) throws IOException
    {
    	Document doc = Jsoup.connect(theChannelListUrl).get();
    	
    	//click continuation buttons, 
    	//then get the first code of "button push"
    	int gettime = 1;
		Elements buttons = doc.select("button[class*=yt-uix-button yt-uix-button-size-default yt-uix-button-default load-more-button yt-uix-load-more browse-items-load-more-button]");
		Elements paths = doc.select("a[class*=pl-video-title-link yt-uix-tile-link yt-uix-sessionlink  spf-link ]");
		
		System.out.println("get:"+gettime);
		
		//取得第一頁影片連結
    	for(Element vPath: paths)
    	{
    		String getHref = vPath.attr("href"); 
    		AllVideo.add("https://www.youtube.com"+getHref);
    		//System.out.println(countAll+" "+getHref);
    		countAll++;
    	}
		
    	//取得第一頁button連結
    	String getpath[] = null;
		for(Element button: buttons)
    	{
    		String click = button.attr("data-uix-load-more-href");
    		getpath = click.split("=");
    		System.out.println(" "+getpath[2]);
    	}
    	//wait for 0.1 sec
    	try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	gettime++;
    	
    	//get the list of "button push" path
		//=====================test=================
    	
    	//若超過100筆影片，按下"載入更多影片"按鈕，繼續往下撈
    	String nextpath = getpath[2];
    	do
    	{
    		try{
    			System.out.println("get:"+gettime);
    			createHtml(nextpath,gettime);
    			nextpath = getButtonPath(gettime);
    			getVideoPath(gettime);
    			gettime++;
    		}catch(Exception e)
    		{
    			getVideoPath(gettime);
    			break;
    		}
    	}while(nextpath!=null);
    	//========================test================
    	//doc = Jsoup.connect("").ignoreContentType(true).execute().parse();
		
    }
    
    
    public static void readContentFromGet(String getURL) throws IOException {
    	// 拼湊get請求的URL字串，使用URLEncoder.encode對特殊和不可見字符進行編碼
        
        URL getUrl = new URL(getURL);
        // 根據拼湊的URL，打開連接，URL.openConnection函數會根據URL的類型，
        // 返回不同的URLConnection子類的對象，這裡URL是一個http，因此實際返回的是HttpURLConnection
        HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
        // 進行連接，但是實際上get request要在下一句的connection.getInputStream()函數中才會真正發到
        // 服務器
        connection.setRequestMethod("GET");
        connection.connect();
        // 取得輸入流，並使用Reader讀取
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        System.out.println("=============================");
//        System.out.println("Contents of get request");
//        System.out.println("=============================");
        String lines;
        String[] get_TextUrl = null;
        while ((lines = reader.readLine()) != null) {
        	if(lines.contains("TTS_URL"))
        	{
        		get_TextUrl = lines.split("[:]");
        	}
            //System.out.println(lines);
        }
        String textUrl = get_TextUrl[1].replaceAll("\"", "")+":"+get_TextUrl[2].replaceAll("[\"]", "");
        textUrl = textUrl.replaceAll("/", "");
        textUrl = textUrl.replaceAll(",", "");
        textUrl = textUrl.replaceAll("\\\\u0026", "&");
        textUrl = textUrl.replaceAll("\\\\", "/");
        textUrl += "&type=track&lang=en&name&kind=asr";
        
        String textUrl2 = textUrl.replaceAll("%2C", ",");
        
        System.out.println("getURL: "+getURL);
        FileWriter f1 = new FileWriter("D:\\Captions\\"+MatchTitle.get(getURL)+".txt");
        
        
        System.out.println("textUrl2: "+textUrl2);
        reader.close();
        // 斷開連接
        connection.disconnect();
//        System.out.println("=============================");
//        System.out.println("Contents of get request ends");
//        System.out.println("=============================");
        
        
        URL url;
        HttpsURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        
        
        try{
        	url = new URL(textUrl);
        	conn = (HttpsURLConnection) url.openConnection();
        	//conn.setRequestMethod("GET");
        	rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        	while((line = rd.readLine()) != null){
        		result += line;
        		result += "\r\n";
        	}
        	//System.out.println(result);
        	f1.write(result);
            f1.flush();
            f1.close();
            
        	rd.close();
        }catch(IOException e){
        	e.printStackTrace();
        }catch(Exception e){
        	e.printStackTrace();
        }
        
//        URL getCaptionUrl = new URL(textUrl);
//        HttpURLConnection connectionCaptionUrl = (HttpURLConnection) getCaptionUrl.openConnection();
//        connectionCaptionUrl.connect();
//        BufferedReader captionReader = new BufferedReader(new InputStreamReader(connectionCaptionUrl.getInputStream()));
//        String getCaptionLines;
//        
//        while ((getCaptionLines = captionReader.readLine()) != null) {
//            System.out.println(getCaptionLines);
//        }
//        captionReader.close();
//        connectionCaptionUrl.disconnect();
    }
	
    public static void readContentFromPost() throws IOException {
        // Post請求的url，與get不同的是不需要帶參數
        URL postUrl = new URL(POST_URL);
        // 打開連接
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        // Output to the connection. Default is
        // false, set to true because post
        // method must write something to the
        // connection
        // 設置是否向connection輸出，因為這個是post請求，參數要放在
        // http正文內，因此需要設為true
        connection.setDoOutput(true);
        // Read from the connection. Default is true.
        connection.setDoInput(true);
        // Set the post method. Default is GET
        connection.setRequestMethod("POST");
        // Post cannot use caches
        // Post 請求不能使用緩存
        connection.setUseCaches(false);
        // This method takes effects to
        // every instances of this class.
        // URLConnection.setFollowRedirects是static函數，作用於所有的URLConnection物件。
        // connection.setFollowRedirects(true);

        // This methods only
        // takes effacts to this
        // instance.
        // URLConnection.setInstanceFollowRedirects是成員函數，僅作用於當前函數
        connection.setInstanceFollowRedirects(true);
        // Set the content type to urlencoded,
        // because we will write
        // some URL-encoded content to the
        // connection. Settings above must be set before connect!
        // 配置本次連接的Content-type，配置為application/x-www-form-urlencoded的
        // 意思是正文是urlencoded編碼過的form參數，下面我們可以看到我們對正文內容使用URLEncoder.encode
        // 進行編碼
        
        
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("step", "9"));
		urlParameters.add(new BasicNameValuePair("functionName", "t147sb02"));
		//urlParameters.add(new BasicNameValuePair("functionName", "t164sb01"));
		urlParameters.add(new BasicNameValuePair("report_id", "B"));
		urlParameters.add(new BasicNameValuePair("co_id", "1101"));
		urlParameters.add(new BasicNameValuePair("year", "2010"));
		urlParameters.add(new BasicNameValuePair("season", "1"));
//				System.err.println("season: <"+ String.valueOf(season)+ ">");
		String parameters = "";
		
		for(NameValuePair parameter : urlParameters)
		{
			parameters += parameter.getName()+ "="+ parameter.getValue()+ "&";
		}

		parameters = parameters.substring(0, parameters.length()-1);

		
        
        
        
        
//        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
//        connection.setRequestProperty("Accept-Language", "h-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,ja;q=0.2");
//        connection.setRequestProperty("Cache-Control", "max-age=0");
//        connection.setRequestProperty("Connection", "keep-alive");
//        connection.setRequestProperty("Content-Length", "57");
//        connection.setRequestProperty("Host", "mops.twse.com.tw");
//        connection.setRequestProperty("Origin", "http://mops.twse.com.tw");
//        connection.setRequestProperty("Referer", "http://mops.twse.com.tw/server-java/t164sb01");
//        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36");
//        connection.setRequestProperty("step", "1");
//        
//        connection.setRequestProperty("step", "9");
//        connection.setRequestProperty("CO_ID", "1101");
//        connection.setRequestProperty("functionName", "t147sb02");
//        connection.setRequestProperty("REPORT_ID", "B");
//        connection.setRequestProperty("YEAR", "2014");
//        connection.setRequestProperty("SEASON", "1");
		
		
        // 連接，從postUrl.openConnection()至此的配置必須要在connect之前完成，
        // 要注意的是connection.getOutputStream會隱含的進行connect。
        connection.connect();
        //DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        // The URL-encoded contend
        // 正文，正文內容其實跟get的URL中'?'後的參數字串一致
        //String content = "firstname=" + URLEncoder.encode("一個大肥人", "utf-8");
        // DataOutputStream.writeBytes將字串中的16位元的unicode字元以8位元的字元形式寫道流裡面
        //out.writeBytes(content);

        //out.flush();
        //out.close(); // flush and close
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        System.out.println("=============================");
        System.out.println("Contents of post request");
        System.out.println("=============================");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println("=============================");
        System.out.println("Contents of post request ends");
        System.out.println("=============================");
        reader.close();
        connection.disconnect();
    }

    
	public static void main(String[] args) throws InterruptedException, IOException
	{
		VideoList(jumpToPlaylist("https://www.youtube.com/user/engadget"));
		for(String videos: MatchTitle.keySet())
		{
			try{
				readContentFromGet(videos);
			}catch(Exception e)
			{
				continue;
			}
			Thread.sleep(1000);
		}
		
		System.out.println(jumpToPlaylist("https://www.youtube.com/user/engadget"));
		
//		for(String v1 : MatchTitle.keySet())
//		{
//			System.out.println(v1+": "+MatchTitle.get(v1));
//			
//		}
	}
}
