package client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
	private static final long serialVersionUID = 4618450393393898072L;
	private String header = null;
	private String text = null;
	private int electionTerm = 0;
	private List<String> list = new ArrayList<String>();

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

	public void setList(List<String> list) {
		this.list = list;
	}

	public List<String> getList() {
		return list;
	}
}
