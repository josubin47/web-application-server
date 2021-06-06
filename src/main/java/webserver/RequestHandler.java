package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import enums.HttpStatus;
import model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private static final String SIGN_UP_URL = "/user/create";
    private static final String LOGIN_URL = "/user/login";
    private static final String LOGIN_FAILED_URL = "/user/login_failed.html";

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = buffer.readLine();

            if(StringUtils.equals(line, null)) {
                return;
            }

            String[] lineToken = line.split(" ");
            String url = lineToken[1]; // request urL

            Map<String, String> params = new HashMap<>();

            int index = url.indexOf("?");

            if(StringUtils.equals(url, "/")) {
                url += "index.html";
            } else if(index != -1) {
                //GET 방식
                //전달된 정보가 있으면
                params = HttpRequestUtils.parseQueryString(url.substring(index+1));
                url = url.substring(0, index);

                log.debug("[url]" + url);
                log.debug("[params]" + params);
            }


            int contentLength = 0;

            //HTTP 정보 읽어오기
            while (!StringUtils.equals(line, "")) {
                line = buffer.readLine();
                log.debug("[line info]" + line);

                if (line.contains("Content-Length")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }


            if(StringUtils.equals(url, SIGN_UP_URL)) {
                //회원가입 정보 저장
                String body = IOUtils.readData(buffer, contentLength);
                params = HttpRequestUtils.parseQueryString(body);
                User userInfo = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                DataBase.addUser(userInfo);

                DataOutputStream dos = new DataOutputStream(out);

                response302Header(dos);
            } else if (StringUtils.equals(url, LOGIN_URL)) {
                String body = IOUtils.readData(buffer, contentLength);
                params = HttpRequestUtils.parseQueryString(body);

                String userId = params.get("userId");
                String password = params.get("password");

                if(DataBase.checkUserInfo(userId, password)){
                    responseLoginSuccessHeader(out);
                } else {
                    url = LOGIN_FAILED_URL;
                    response200Header(out, url);
                }

            } else {
                response200Header(out, url);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(OutputStream out, String url) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);

        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());

        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLoginSuccessHeader(OutputStream out) {
        try {
            DataOutputStream dos = new DataOutputStream(out);

            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("Set-Cookie: logined=true \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

//    private void responseHeader(DataOutputStream dos, int lengthOfBodyContent, HttpStatus statusCode) {
//        try {
//
//            String statusLine = "";
//            String location = ""; //302
//
//            switch (statusCode) {
//                case OK:
//                    statusLine = "HTTP/1.1 200 OK \r\n";
//                    break;
//                case MOVED_TEMPORARILY:
//                    statusLine = "HTTP/1.1 302 Redirect \r\n";
//                    location = "Location: /index.html \r\n";
//                    break;
//            }
//
//            dos.writeBytes(statusLine);
//            if(!StringUtils.isEmpty(location)) {
//                dos.writeBytes(location);
//            }
//            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
//            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
//            dos.writeBytes("\r\n");
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
