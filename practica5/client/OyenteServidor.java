package practica5.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import practica5.estructuras.Lock;
import practica5.mensaje.Mensaje;
import practica5.mensaje.MensajeCerrarConexion;
import practica5.mensaje.MensajeCerrarConexionConfirmacion;
import practica5.mensaje.MensajeConexion;
import practica5.mensaje.MensajeConfirmacionConexion;
import practica5.mensaje.MensajeConfirmacionListaUsuarios;
import practica5.mensaje.MensajeEmitirCiudad;
import practica5.mensaje.MensajeListaUsuarios;
import practica5.mensaje.MensajePaisConsultadoConfirmacion;
import practica5.mensaje.MensajePedirCiudad;
import practica5.mensaje.MensajePreparadoCS;
import practica5.mensaje.MensajePreparadoSC;

public class OyenteServidor extends Thread {
	
	private Socket sc;
	private Usuario usr;
	private Lock l;
	private ObjectInputStream finc;
	private ObjectOutputStream foutc;
	private ArrayList<Emisor> emisores;
	private ArrayList<Receptor> receptores;
	private String ciudadPreguntada;
	private boolean listening;

	public OyenteServidor(Socket sc, Usuario usr,Lock l) {
		this.sc = sc;
		this.usr = usr;
		this.l = l;
		this.emisores = new ArrayList<Emisor>();
		this.receptores = new ArrayList<Receptor>();
		listening = true;
	}
	
	public void run() {
		try {

			foutc = new ObjectOutputStream(sc.getOutputStream());
			foutc.writeObject(new MensajeConexion(usr.getIP(), "localhost", usr.getNombre(), usr.getMapa().getMapa()));
			foutc.flush(); 
			
			finc = new ObjectInputStream(sc.getInputStream());
			
			while(listening) {
				Mensaje m = (Mensaje) finc.readObject(); //escuchamos el canal y leemos el mensaje.
				// solo ponemos los mensajes que el cliente va a leer.
				switch(m.getTipoM()) {
				case M_CONFIRMACION_CONEXION:
					gestionarMensajeConfirmacionConexion((MensajeConfirmacionConexion) m);
					break;
					
				case M_CONFIRMACION_LISTA_USUARIOS:
					gestionarMensajeConfirmacionListaUsuarios((MensajeConfirmacionListaUsuarios) m);
					break;
					
				case M_EMITIR_CIUDAD:
					gestionarMensajeEmisionCiudad((MensajeEmitirCiudad) m);
					break;
					
				case M_PREPARADO_SC:
					gestionarMensajePreparadoSC((MensajePreparadoSC) m);
					break;
				case M_PAIS_CONSULTADO_CONFIRMACION:
					gestionarMensajeConfirmacionPaisConsultado((MensajePaisConsultadoConfirmacion) m);
					break;
					
				case M_CONFIRMACION_CERRAR_CONEXION:
					gesionarMensajeConfirmacionCerrarConexion((MensajeCerrarConexionConfirmacion) m);
					break;
				default:
					break;
				}
				//habra que hacer switch con los tipos de mensaje.
			}
			for(int i = 0; i < emisores.size(); i++) {
				emisores.get(i).join();
			}
			for(int i = 0; i < receptores.size(); i++) {
				receptores.get(i).join();
			}
			finc.close();
			foutc.close();
		}catch(Exception e) {
			
		}
	}
	

	public void gestionarMensajeConfirmacionConexion(MensajeConfirmacionConexion m) {
		
		if(m.getConectado()) { // se ha conectado con exito
			// aquí hay algo más que hacer?????
			l.takeLock(2);
			System.out.println("Se ha realizado la conexión con el servidor con éxito.");
			l.releaseLock(2);
		}else {
			l.takeLock(2);
			System.out.println("Error. No se ha podido establecer la conexión con el servidor.");
			l.releaseLock(2);
			listening = false;
		}
	}
	
	private void gestionarMensajeConfirmacionListaUsuarios(MensajeConfirmacionListaUsuarios m) {
		l.takeLock(2);
		System.out.println(m.getListaUsuariosString());
		l.releaseLock(2);
	}
	
	private void gestionarMensajeEmisionCiudad(MensajeEmitirCiudad m) throws IOException, InterruptedException {
		String p = usr.getMapa().getPais(m.getCiudad());
		Emisor e = new Emisor(p);
		emisores.add(e);
		e.start();
		foutc.writeObject(new MensajePreparadoCS(usr.getIP(), "localhost", m.getNombreReceptor(), e.getPuerto()));
		foutc.flush();
	}

	private void gestionarMensajePreparadoSC(MensajePreparadoSC m) throws IOException {
		Receptor r = new Receptor(m.getPuerto(), m.getIPEmisor(), ciudadPreguntada, usr, foutc);
		receptores.add(r); //lo añadimos a la lista de receptores
		r.start();
	}
	
	private void gestionarMensajeConfirmacionPaisConsultado(MensajePaisConsultadoConfirmacion m) {
		
		l.takeLock(2);
		System.out.println("Pais recibido.");
		System.out.println("Se ha añadido el par, ciudad: " + m.getCiudad() + ", pais: " + m.getPais() + " a la lista de información propia.");
		l.releaseLock(2);
		
	}
	
	private void gesionarMensajeConfirmacionCerrarConexion(MensajeCerrarConexionConfirmacion m) {
		listening = false;
		l.takeLock(2);
		System.out.println("El usuario " + usr.getNombre() + " ha cerrado con éxito la conexión el servidor.");
		l.releaseLock(2);		
	}
	
	public void mandarMensajeCierreConexion() throws IOException {
		foutc.writeObject(new MensajeCerrarConexion(usr.getIP(), "localhost")); //mandamos mensaje de cierre de conexion
		foutc.flush();
	}
	
	public void mandarMensajeListaUsuarios() throws IOException {
		foutc.writeObject(new MensajeListaUsuarios(usr.getIP(), "localhost")); //mandamos mensaje de lista de usuarios
		foutc.flush();
	}
	
	public void mandarMensajePedirCiudad(String ciudad, String nombreUsuario) throws IOException {
		ciudadPreguntada = ciudad;
		foutc.writeObject(new MensajePedirCiudad(usr.getIP(), "localhost", ciudad, nombreUsuario)); //mandamos mensaje de pedir el pais de la ciudad
		foutc.flush();
	}
	
}

