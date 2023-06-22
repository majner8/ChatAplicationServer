package Users;

import CommonPart.SocketComunication.SocketComunication;
import Users.UserConnection.DoNotSendMessageToSenderSocketComunicationMessage;

public class GroupChat {

	public static void ProcessMessage(SocketComunication message,boolean OutPut) {}
	
	private void OutPutMessage(SocketComunication message) {
	}
	
	

	/** Metod manage send message to all conected device, if user do not have connected device, nothing happen
	 * @param UUIDofSender- if you do not wan to send message back to sender
	 * in the other hand, put null*/
	public void InputMessage(SocketComunication message,String UUIDofSender) {
	}

}
