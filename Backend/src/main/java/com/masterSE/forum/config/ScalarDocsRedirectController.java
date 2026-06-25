package com.masterSE.forum.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ScalarDocsRedirectController {

	@GetMapping("/docs")
	public String scalarDocs() {
		return "redirect:/scalar.html";
	}
}
