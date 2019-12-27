package client;

import java.io.Serializable;
import java.util.Vector;

public class Message implements Serializable {
	private static final long serialVersionUID = 4618450393393898072L;
	private String header = null;
	private String text = null;
	private int sequenceNumber = 0;
	private int electionTerm = 0;
	private Vector<String> stringList = new Vector<String>();
	private Vector<Message> messageList = new Vector<Message>();

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setElectionTerm(int electionTerm) {
		this.electionTerm = electionTerm;
	}

	public int getElectionTerm() {
		return electionTerm;
	}

	public void setStringList(Vector<String> list) {
		synchronized (stringList) {
			this.stringList = list;
		}
	}

	public Vector<String> getStringList() {
		synchronized (stringList) {
			return stringList;
		}
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setMessageList(Vector<Message> list) {
		synchronized (messageList) {
			this.messageList = list;
		}
	}

	public Vector<Message> getMessageList() {
		synchronized (messageList) {
			return messageList;
		}
	}
}
