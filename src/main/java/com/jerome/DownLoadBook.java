package com.jerome;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.gargoylesoftware.htmlunit.ConfirmHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindowEvent;
import com.gargoylesoftware.htmlunit.WebWindowListener;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class DownLoadBook {
	private static Logger logger = Logger.getLogger(DownLoadBook.class);

	private WebDriver driver = null;

	// please fill in your name and pwd
	private String loginUrl = "";
	private String name = "";
	private String pwd = "";
	private String downLoadUrl = "";
	private String filePath = "";

	private String name_Id = "name";
	private String pwd_Id = "pw";
	private String signIn = "//button[@value = 'submit']";
	private String selectInv = "//button[@value = 'submit']";

	public DownLoadBook(String loginUrl, String name, String pwd,
			String downLoadUrl, String filePath) {
		this.loginUrl = loginUrl;
		this.name = name;
		this.pwd = pwd;
		this.downLoadUrl = downLoadUrl;
		SimpleDateFormat dateformat1=new SimpleDateFormat("yyyy-MM-dd");
		this.filePath = filePath+File.separator+dateformat1.format(new Date())+File.separator;
		driver = new CustomHtmlUnitDriver();
		driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
		
		if (!new File(this.filePath).exists()) {
			new File(this.filePath).mkdirs();
		}
	}

	private void Login() throws Exception {
		driver.get(loginUrl);
		driver.findElement(By.id(name_Id)).sendKeys(name);
		driver.findElement(By.id(pwd_Id)).sendKeys(pwd);
		driver.findElement(By.xpath(signIn)).submit();
		new WebDriverWait(driver, 10).until(ExpectedConditions
				.visibilityOf(driver.findElement(By.name("target_site"))));

		new Select(driver.findElement(By.name("target_site")))
				.selectByVisibleText("Investopedia");

		driver.findElement(By.xpath(this.selectInv)).submit();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// new WebDriverWait(driver, 10).until(ExpectedConditions.)));
		// driver.findElement(By.id(name_Id)).
		String body = (driver.getPageSource());
		if (!body.contains("Content")) {
			throw new Exception("No Content link");
		}

	}

	public void DownLoadfile() throws Exception {
		Login();
		driver.get("https://external-analyze.ask.com/t/investopedia/workbooks");
		// FileWriter fw = new FileWriter("./a.html");
		// PrintWriter pw = new PrintWriter(fw);
		// pw.println(driver.getPageSource());
		// pw.flush();pw.close();
		List<WebElement> downloads = driver.findElements(By
				.className("downloadVizText"));
		List<String> downloadLinks = new ArrayList<String>();
		for (WebElement downloadLink : downloads) {
			downloadLinks.add(downloadLink.getAttribute("href"));

		}

		for (String url : downloadLinks) {
			System.out.println(url);
			driver.get(url);
		}
		// System.out.println(driver.getPageSource());
		// driver.get(downLoadUrl);

	}

	public static void main(String[] args) throws Exception {
		DownLoadBook dlb = new DownLoadBook(
				"https://external-analyze.ask.com/auth/change_user?language=en",
				"tanj",
				"123!@#QWEqwe",
				"https://external-analyze.ask.com/t/investopedia/workbooks/Masterdownload?format=twb&errfmt=html",
				args[0]);
		dlb.DownLoadfile();
	}

	private class CustomHtmlUnitDriver extends HtmlUnitDriver {

		// This is the magic. Keep a reference to the client instance
		protected WebClient modifyWebClient(WebClient client) {

			ConfirmHandler okHandler = new ConfirmHandler() {
				public boolean handleConfirm(Page page, String message) {
					return true;
				}
			};
			client.setConfirmHandler(okHandler);

			client.addWebWindowListener(new WebWindowListener() {

				public void webWindowOpened(WebWindowEvent event) {

				}

				public void webWindowContentChanged(WebWindowEvent event) {

					WebResponse response = event.getWebWindow()
							.getEnclosedPage().getWebResponse();

					// Change or add conditions for content-types that you would
					// to like
					// receive like a file.
					if (response.getContentType().equals("application/x-twb")) {
						getFileResponse(response, filePath);
					}

				}

				public void webWindowClosed(WebWindowEvent event) {

				}
			});

			return client;
		}

	}

	private void getFileResponse(WebResponse response, String fileName) {

		InputStream inputStream = null;

		// write the inputStream to a FileOutputStream
		OutputStream outputStream = null;

		try {
			String distFileName = UUID.randomUUID().toString() + ".twb";
			inputStream = response.getContentAsStream();
			List<NameValuePair> nvs = (response.getResponseHeaders());
			try {
				for (NameValuePair nv : nvs) {
					// System.out.println(nv.getName() + "==>" + nv.getValue());
					if (nv.getName().toLowerCase().trim()
							.equals("content-disposition")) {
						distFileName = (java.net.URLDecoder.decode(nv
								.getValue().split("filename")[1].replace("\"",
								"").replace("=", "")));
					}
				}
			} catch (Exception e) {
			}
			// write the inputStream to a FileOutputStream
			outputStream = new FileOutputStream(new File(filePath
					+ File.separator + distFileName));

			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}

			logger.info("get file Done!");

		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
			if (outputStream != null) {
				try {
					// outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}

			}
		}

	}

}
