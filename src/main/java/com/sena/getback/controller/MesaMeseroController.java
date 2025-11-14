package com.sena.getback.controller;

import com.sena.getback.model.Mesa;
import com.sena.getback.model.Location;
import com.sena.getback.service.MesaService;
import com.sena.getback.service.LocationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/mesero")
public class MesaMeseroController {

    @Autowired
    private MesaService mesaService;

    @Autowired
    private LocationService locationService;

    @GetMapping("/mesas")
    public String verMesasMesero(Model model) {
        List<Mesa> mesas = mesaService.findAll();
        List<Location> ubicaciones = locationService.findAll();

        model.addAttribute("mesas", mesas);
        model.addAttribute("ubicaciones", ubicaciones);
        return "fusionMesasMesero"; 
    }
    
    
}