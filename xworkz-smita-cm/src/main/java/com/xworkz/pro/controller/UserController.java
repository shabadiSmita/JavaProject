package com.xworkz.pro.controller;

import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.xworkz.pro.dto.UserDTO;
import com.xworkz.pro.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/")
public class UserController {

	@Autowired
	private UserService userService;

	public UserController() {
		log.info("" + this.getClass().getSimpleName());
	}

	@PostMapping("/save")
	public String userInfo(UserDTO userDTO, Model model) {
		Set<ConstraintViolation<UserDTO>> violations = this.userService.validateAndSave(userDTO);

		if (violations != null && violations.isEmpty() && userDTO != null) {
			model.addAttribute("message", "Registration sucessfull");
			log.info("Running in no violations condition ");
			log.info("" + userDTO);
			return "signUp";
		}
		model.addAttribute("errors", violations);
		model.addAttribute("messag", "Registration failed");
		model.addAttribute("dto", userDTO);
		log.info("Running in  violations condition ");
		return "signUp";

	}

	@PostMapping("/signin")
	public String userSignIn(String userId, String password, Model model, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		log.info("Running in userSignIn condition ");
		try {
			UserDTO uudto = this.userService.userSignIn(userId, password,model);
			log.info("Login count" + uudto.getLoginCount());
			if (uudto.getLoginCount() >= 3) {
				model.addAttribute("msg", "Account locked Reset password");
				log.info("Acount locked due to wrong password entering 3 times");
				return "SignIn";
			}
			if (uudto != null) {

				if (uudto.getResetPassword() == true) {
					log.info("Running in ResetPassword true condition");
					if (!uudto.getPasswordChangedTime().isAfter(LocalTime.now())) {
						log.info("Running in time varifying condition");
						model.addAttribute("msgs", "Time out plz try again");
						return "SignIn";
					}
					model.addAttribute("userID", uudto.getUserId());
					log.info("Running in reset condition");
					log.info("ResetPassword---" + uudto.getResetPassword());
					log.info("Timer-----" + uudto.getPasswordChangedTime().isBefore(LocalTime.now()));
					return "updatePassword";
				}
				log.info("User ID and password is matched");
				// model.addAttribute("userID", udto.getUserId());//request scope
				HttpSession httpSession = request.getSession(true);
				httpSession.setAttribute("userID", uudto.getUserId());
				if (uudto.getPicName() != null) {
					httpSession.setAttribute("dtoPic", uudto.getPicName());
				}
				httpSession.setAttribute("udto", uudto);
				return "LoginSucess";
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.info(e.getMessage());
		}
		log.info("UserID OR Password is not matching");
		model.addAttribute("match", "UserID OR Password is not matching");
		return "SignIn";

	}

	@PostMapping("/reset")
	public String reSetPassword(String email, Model model) {
		try {
			UserDTO udto = this.userService.reSetPassword(email);
			if (udto.getResetPassword() == true) {
				log.info("Password reset sucessful plz login within 2 min with otp");
				model.addAttribute("msg", "Password reset sucessful plz login within 2 min with otp");
				return "resetpassword";
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.info(e.getMessage());
		}
		return "resetpassword";

	}

	@PostMapping("/passwordUpdate")
	public String upDatePassword(String userId, String password, String confirmPassword) {
		log.info("Running in upDatePassword method");
		this.userService.updatePassword(userId, password, confirmPassword);
		return "SignIn";
	}

	@PostMapping("/upload")
	public String onUpload(@RequestParam("chitra") MultipartFile multipartFile, String userId, String email,
			Long mobile, Model model) throws IOException {
		log.info("multipartFile" + multipartFile);
		log.info(multipartFile.getOriginalFilename());
		log.info(multipartFile.getContentType());
		log.info("Size of file" + multipartFile.getSize());
		log.info("Size of bite" + multipartFile.getBytes());
		if (multipartFile.isEmpty()) {
			log.info("file not uploaded");
			model.addAttribute("error", "please select file");
			return "profileUpdate";
		}
		String imageFormat = multipartFile.getOriginalFilename().substring(
				multipartFile.getOriginalFilename().lastIndexOf('.'), multipartFile.getOriginalFilename().length());
		log.info("Image last form" + imageFormat);
		model.addAttribute("sucess", "image uploaded sucessfully");
		byte[] bytes = multipartFile.getBytes();
		Path path = Paths.get("C:\\highway\\" + userId + System.currentTimeMillis() + imageFormat);
		Files.write(path, bytes);
		String imageName = path.getFileName().toString();
		log.info("Image name--" + imageName);
		log.info("Image name in tostring--" + path.toString());
		log.info("Image file name--" + path.getFileName());
		this.userService.updateProfile(userId, email, mobile, imageName);
		return "profileUpdate";
	}

	@GetMapping("/download")
	public void onDownload(HttpServletResponse response, @RequestParam String fileName, UserDTO user)
			throws IOException {
		try {
			Path path = Paths.get("C:\\highway\\" + user.getPicName());
			path.toFile();
			response.setContentType("image/jpeg");
			File file = new File("C:\\highway\\" + fileName);
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			ServletOutputStream out = response.getOutputStream();
			IOUtils.copy(in, out);
			response.flushBuffer();
		} catch (FileNotFoundException e) {
			e.getStackTrace();
		}
	}
}
