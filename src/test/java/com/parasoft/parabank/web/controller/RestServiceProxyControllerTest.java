package com.parasoft.parabank.web.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.parasoft.parabank.domain.Account;
import com.parasoft.parabank.domain.Customer;
import com.parasoft.parabank.domain.Transaction;
import com.parasoft.parabank.domain.logic.BankManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/**/service-proxy-test-context.xml" })
@WebAppConfiguration
@Transactional
public class RestServiceProxyControllerTest {

	private MockMvc mockMvc;
	
	@Autowired
	private WebApplicationContext wac;
	
	@Resource(name = "bankManager")
    protected BankManager bankManager;
	
	private int customerId = 12212;
	
	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}
	
	public final void setBankManager(final BankManager bankManager) {
        this.bankManager = bankManager;
    }
	
	@Test
	public void testGetAccounts() throws Exception {
		Customer customer = bankManager.getCustomer(customerId);	
		List<Account> accounts = bankManager.getAccountsForCustomer(customer);
		assertNotNull(accounts);
		assertFalse(accounts.isEmpty());
		mockMvc.perform(
				get("/bank/customers/" + customerId + "/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$", hasSize(accounts.size())
		));
	}
	
	@Test
	public void testGetAccount() throws Exception {
		mockMvc.perform(get("/bank/accounts/12456")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.availableBalance", is(10.45)))
		.andExpect(jsonPath("$.customerId", is(customerId)))
		.andExpect(jsonPath("$.type", is("CHECKING")));
	}
	
	@Test
	public void testGetTransactions() throws Exception{
		Account account = bankManager.getAccount(12456);
		assertNotNull(account);
		List<Transaction> transactions = bankManager.getTransactionsForAccount(account);
		assertNotNull(transactions);
		mockMvc.perform(
				get("/bank/accounts/12456/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$", hasSize(transactions.size())));
	}
	
	@Test
	public void testTransferAndGetTransactionByMonthAndType() throws Exception {
		//perform a transaction (i.e. transfer 10 from account 12567 to 12456)
		mockMvc.perform(
				post("/bank/transfer")
				.contentType(MediaType.APPLICATION_JSON)
				.param("fromAccountId", "12567")
				.param("toAccountId", "12456")
				.param("amount", "10.00")
				.content("")
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk());
		
		//GET for transactions should return the transfer from the POST
		mockMvc.perform(
				get("/bank/accounts/12567/transactions/month/All/type/All")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$", hasSize(1)))
		.andExpect(jsonPath("$[0].amount", is(10.0)))
		.andExpect(jsonPath("$[0].description", is("Funds Transfer Sent")))
		.andExpect(jsonPath("$[0].type", is("Debit")));
		
		//GET for transactions of type 'Credit' should return empty since
		//transfer is a 'Debit' transaction
		mockMvc.perform(
				get("/bank/accounts/12567/transactions/month/All/type/Credit")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$", hasSize(0)));
	}
	
	@Test
	public void testCreateAccount() throws Exception {
		mockMvc.perform(
				post("/bank/createAccount")
				.contentType(MediaType.APPLICATION_JSON)
				.param("customerId", "" + customerId)
				.param("newAccountType", "0")
				.param("fromAccountId", "13122")
				.content("")
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.availableBalance", is(0)))
		.andExpect(jsonPath("$.customerId", is(customerId)))
		.andExpect(jsonPath("$.type", is("CHECKING")));
	}
	
	@Test
	public void testRequestLoan() throws Exception {
		mockMvc.perform(
				post("/bank/requestLoan")
				.contentType(MediaType.APPLICATION_JSON)
				.param("customerId", "" + customerId)
				.param("amount", "1000")
				.param("downPayment", "200")
				.param("fromAccountId", "13122")
				.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.approved", is(true)));
	}
}