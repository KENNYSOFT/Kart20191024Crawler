package kr.KENNYSOFT.Kart20191024Crawler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Kart20191024Crawler
{
	public static JSONParser jsonParser = new JSONParser();
	public static final String cntstr = " \\((\\d+)개\\)$";
	public static final Pattern pattern = Pattern.compile(cntstr);
	public static final String ENC = "YOUR_ENC";
	public static final String KENC = "YOUR_KENC";
	public static final String NPP = "YOUR_NPP";
	
	public static void main(String[] args) throws Exception
	{
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		List<HttpCookie> cookieList = new ArrayList<>();
		HttpCookie cookie = new HttpCookie("ENC", ENC);
		cookie.setDomain("nexon.com");
		cookie.setPath("/");
		cookie.setVersion(0);
		cookieList.add(cookie);
		cookie = new HttpCookie("KENC", KENC);
		cookie.setDomain("kart.nexon.com");
		cookie.setPath("/");
		cookie.setVersion(0);
		cookieList.add(cookie);
		cookie = new HttpCookie("NPP", NPP);
		cookie.setDomain("nexon.com");
		cookie.setPath("/");
		cookie.setVersion(0);
		cookieList.add(cookie);
		for (HttpCookie c : cookieList) cookieManager.getCookieStore().add(URI.create("http://kart.nexon.com/"), c);
		CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get("coupon.csv")), CSVFormat.DEFAULT.withHeader("\uFEFF당첨일", "아이템", "수량", "유효기간", "쿠폰번호"));
		while (true)
		{
			boolean success = false;
			boolean completed;
			do
			{
				completed = true;
				try
				{
					success = play();
				}
				catch (HttpRetryException e)
				{
					for (HttpCookie c : cookieList) cookieManager.getCookieStore().add(URI.create("http://kart.nexon.com/"), c);
					completed = false;
				}
			} while (!completed);
			if (!success) break;
		}
		int pages = 1;
		for (int i = 1; i <= pages; ++i)
		{
			Document html = null;
			boolean completed;
			do
			{
				completed = true;
				try
				{
					html = list(i);
				}
				catch (HttpRetryException e)
				{
					for (HttpCookie c : cookieList) cookieManager.getCookieStore().add(URI.create("http://kart.nexon.com/"), c);
					completed = false;
				}
			} while (!completed);
			try
			{
				pages = Integer.parseInt(html.selectFirst(".btn.btn_ls").attr("onclick").replaceAll("[^\\d]", ""));
			}
			catch (Exception e)
			{
			}
			Elements array = html.select(".coupon");
			for (Element elem : array)
			{
				Matcher matcher = pattern.matcher(elem.selectFirst(".name").text());
				csvPrinter.printRecord(elem.selectFirst(".date").text(), elem.selectFirst(".name").text().replaceAll(cntstr, ""), matcher.find() ? matcher.group(1) : "-", elem.selectFirst(".validity").text(), elem.selectFirst(".key").text());
			}
		}
		csvPrinter.flush();
		csvPrinter.close();
	}
	
	public static boolean play() throws Exception
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://kart.nexon.com/events/2019/1024/Play.aspx").openConnection();
		conn.setRequestMethod("POST");
		conn.setFixedLengthStreamingMode(0);
		conn.setRequestProperty("Referer", "http://kart.nexon.com/events/2019/1024/Event.aspx");
		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		conn.setDoOutput(true);
		DataOutputStream os = new DataOutputStream(conn.getOutputStream());
		os.flush();
		os.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuffer response = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) response.append(line);
		br.close();
		conn.disconnect();
		System.out.println(response);
		try
		{
			return (long) ((JSONObject) jsonParser.parse(response.toString())).get("retCode") == 0;
		}
		catch (Exception e)
		{
			throw new HttpRetryException("", 0);
		}
	}
	
	public static Document list(int page) throws Exception
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://kart.nexon.com/events/2019/1024/MyCouponList.aspx").openConnection();
		Map<String, String> parameters = new HashMap<>();
		parameters.put("n4Page", String.valueOf(page));
		StringJoiner sj = new StringJoiner("&");
		for (Entry<String, String> entry : parameters.entrySet()) sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
		byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
		conn.setRequestMethod("POST");
		conn.setFixedLengthStreamingMode(out.length);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Referer", "http://kart.nexon.com/events/2019/1024/Event.aspx");
		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		conn.setDoOutput(true);
		DataOutputStream os = new DataOutputStream(conn.getOutputStream());
		os.write(out);
		os.flush();
		os.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuffer response = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) response.append(line);
		br.close();
		conn.disconnect();
		System.out.println(response);
		return Jsoup.parse(response.toString());
	}
}