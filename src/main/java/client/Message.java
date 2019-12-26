package client;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Message implements Serializable {
	private static final long serialVersionUID = 4618450393393898072L;
	private String header = null;
	private String text = null;
	private AtomicInteger sequenceNumber = new AtomicInteger(0);
	private int electionTerm = 0;
	private CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public synchronized String getText() {
		return text;
	}

	public synchronized void setText(String text) {
		this.text = text;
	}

	public void setElectionTerm(int electionTerm) {
		this.electionTerm = electionTerm;
	}

	public int getElectionTerm() {
		return electionTerm;
	}

	public void setList(CopyOnWriteArrayList<String> list) {
		synchronized (list) {
			this.list = list;
		}
	}

	public List<String> getList() {
		synchronized (list) {
			return list;
		}
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber.set(sequenceNumber);
	}

	public int getSequenceNumber() {
		return sequenceNumber.get();
	}
}
