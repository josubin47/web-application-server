package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
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

    private static final String SIGN_UP_PATH = "/user/create";
    private static final String LOGIN_PATH = "/user/login";
    private static final String USER_LIST_PATH = "/user/list";
    private static final String LOGIN_URL = "/user/login.html";
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

            if (StringUtils.equals(line, null)) {
                return;
            }

            String[] lineToken = line.split(" ");
            String url = lineToken[1]; // request urL

            Map<String, String> params = new HashMap<>();

            int index = url.indexOf("?");

            if (StringUtils.equals(url, "/")) {
                url += "index.html";
            } else if (index != -1) {
                //GET 방식
                //전달된 정보가 있으면
                params = HttpRequestUtils.parseQueryString(url.substring(index + 1));
                url = url.substring(0, index);

                log.debug("[url]" + url);
                log.debug("[params]" + params);
            }


            int contentLength = 0;
            boolean isLogin = false;

            //HTTP 정보 읽어오기
            while (!StringUtils.equals(line, "")) {
                line = buffer.readLine();
                log.debug("[line info]" + line);

                if (line.contains("Content-Length")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }

                if (line.contains("Cookie")) {
                    isLogin = isLogin(line);
                }
            }

            switch (url) {
                case SIGN_UP_PATH: {
                    //회원가입 정보 저장
                    String body = IOUtils.readData(buffer, contentLength);
                    params = HttpRequestUtils.parseQueryString(body);
                    User userInfo = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                    DataBase.addUser(userInfo);

                    response302Header(out);
                    break;
                }
                case LOGIN_PATH: {
                    String body = IOUtils.readData(buffer, contentLength);
                    params = HttpRequestUtils.parseQueryString(body);

                    String userId = params.get("userId");
                    String password = params.get("password");

                    if (DataBase.checkUserInfo(userId, password)) {
                        responseLoginSuccessHeader(out);
                    } else {
                        url = LOGIN_FAILED_URL;
                        response200Header(out, url, "html");
                    }
                    break;
                }
                case USER_LIST_PATH: {
                    if (isLogin) {
                        //로그인 된 상태
                        Collection<User> userList = DataBase.findAll();
                        StringBuilder stringBuilder = new StringBuilder();

                        userList.forEach(user ->
                                {
                                    stringBuilder.append("<p> 사용자 아이디 : ").append(user.getUserId()).append("</p>");
                                    stringBuilder.append("<p> 사용자 이름 : ").append(user.getName()).append("</p><br/><br/>");
                                }
                        );

                        DataOutputStream dos = new DataOutputStream(out);

                        byte[] body = stringBuilder.toString().getBytes();

                        response200Header(dos, body.length, "html");
                        responseBody(dos, body);

                    } else {
                        url = LOGIN_URL;
                        response200Header(out, url, "html");
                    }
                    break;
                }
                default:
                    if (url.endsWith(".css")) {
                        response200Header(out, url, "css");
                    } else {
                        response200Header(out, url, "html");
                    }
                    break;
            }

            in.close();
            out.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isLogin(String line) {
        Map<String, String> cookies = HttpRequestUtils.parseCookies(line.split(":")[1].trim());
        return StringUtils.equals(cookies.get("logined"), "true");

    }

    private void response200Header(OutputStream out, String url, String contentType) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);

        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());

        response200Header(dos, body.length, contentType);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) {
        try {
            String contentTypeLine = "";

            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            switch (contentType) {
                case "html" : {
                    contentTypeLine = "Content-Type: text/html;charset=utf-8\r\n";
                    break;
                }
                case "css" : {
                    contentTypeLine = "Content-Type: text/css;charset=utf-8\r\n";
                    break;
                }
            }

            dos.writeBytes(contentTypeLine);
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(OutputStream out) {
        try {
            DataOutputStream dos = new DataOutputStream(out);

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
