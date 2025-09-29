package com.sena.getback.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home() {
		// Redirige al panel de administración centralizado
		return "index";
	}
}
