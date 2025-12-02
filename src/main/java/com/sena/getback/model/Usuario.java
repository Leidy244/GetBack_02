package com.sena.getback.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String apellido;
	private String telefono;
	private String direccion;
	private String correo;
	private String clave;
	private String foto;
	private LocalTime horaInicio;
	private LocalTime horaFin;
	private LocalDate fecha;
	private String estado = "ACTIVO";

	@Column(name = "reset_password_token")
	private String resetPasswordToken;

	@Column(name = "reset_password_expires")
	private LocalDateTime resetPasswordExpires;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "rol_id")
	private Rol rol;

	// Constructores
	public Usuario() {
	}

	public Usuario(Long id, String nombre, String apellido, String telefono, String direccion, String correo,
			String clave, String foto, LocalTime horaInicio, LocalTime horaFin, LocalDate fecha, String estado,
			Rol rol) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.apellido = apellido;
		this.telefono = telefono;
		this.direccion = direccion;
		this.correo = correo;
		this.clave = clave;
		this.foto = foto;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
		this.fecha = fecha;
		this.estado = estado;
		this.rol = rol;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	public String getFoto() {
		return foto;
	}

	public void setFoto(String foto) {
		this.foto = foto;
	}

	public LocalTime getHoraInicio() {
		return horaInicio;
	}

	public void setHoraInicio(LocalTime horaInicio) {
		this.horaInicio = horaInicio;
	}

	public LocalTime getHoraFin() {
		return horaFin;
	}

	public void setHoraFin(LocalTime horaFin) {
		this.horaFin = horaFin;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public String getResetPasswordToken() {
		return resetPasswordToken;
	}

	public void setResetPasswordToken(String resetPasswordToken) {
		this.resetPasswordToken = resetPasswordToken;
	}

	public LocalDateTime getResetPasswordExpires() {
		return resetPasswordExpires;
	}

	public void setResetPasswordExpires(LocalDateTime resetPasswordExpires) {
		this.resetPasswordExpires = resetPasswordExpires;
	}

	@Override
	public String toString() {
		return "Usuario [id=" + id + ", nombre=" + nombre + ", apellido=" + apellido + ", telefono=" + telefono
				+ ", direccion=" + direccion + ", correo=" + correo + ", clave=" + clave + ", foto=" + foto
				+ ", horaInicio=" + horaInicio + ", horaFin=" + horaFin + ", fecha=" + fecha + ", estado=" + estado
				+ ", resetPasswordToken=" + resetPasswordToken + ", resetPasswordExpires=" + resetPasswordExpires + "]";
	}

}