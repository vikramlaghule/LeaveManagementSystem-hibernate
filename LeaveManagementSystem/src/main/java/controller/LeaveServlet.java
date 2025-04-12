package controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import model.Leave;
import model.User;
import util.HibernateUtil;

@WebServlet("/leave/*")
public class LeaveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession httpSession = request.getSession(false);
        
        if (httpSession == null || httpSession.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }
        
        int userId = (int) httpSession.getAttribute("userId");
        String action = request.getParameter("action");
        
        if ("status".equals(action)) {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.get(User.class, userId);
                if (user != null) {
                    response.setContentType("text/html");
                    response.getWriter().write("<p>Paid Leave: " + user.getPaidLeave() + "</p>");
                    response.getWriter().write("<p>Sick Leave: " + user.getSickLeave() + "</p>");
                    response.getWriter().write("<p>Casual Leave: " + user.getCasualLeave() + "</p>");
                    
                    // Get leave history
                    Query<Leave> query = session.createQuery(
                        "FROM Leave WHERE user.id = :userId ORDER BY appliedDate DESC", Leave.class);
                    query.setParameter("userId", userId);
                    List<Leave> leaves = query.list();
                    
                    if (!leaves.isEmpty()) {
                        response.getWriter().write("<h3>Leave History</h3>");
                        response.getWriter().write("<table border='1'>");
                        response.getWriter().write("<tr><th>Type</th><th>Days</th><th>Applied Date</th></tr>");
                        
                        for (Leave leave : leaves) {
                            response.getWriter().write("<tr>");
                            response.getWriter().write("<td>" + leave.getType() + "</td>");
                            response.getWriter().write("<td>" + leave.getDays() + "</td>");
                            response.getWriter().write("<td>" + leave.getAppliedDate() + "</td>");
                            response.getWriter().write("</tr>");
                        }
                        
                        response.getWriter().write("</table>");
                    } else {
                        response.getWriter().write("<p>No leave history found</p>");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().write("Error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession httpSession = request.getSession(false);
        
        if (httpSession == null || httpSession.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }
        
        int userId = (int) httpSession.getAttribute("userId");
        String leaveType = request.getParameter("leaveType");
        int days;
        
        try {
            days = Integer.parseInt(request.getParameter("days"));
        } catch (NumberFormatException e) {
            response.sendRedirect("apply-leave.html?error=invalid_days");
            return;
        }
        
        if (days <= 0) {
            response.sendRedirect("apply-leave.html?error=zero_days");
            return;
        }
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            
            try {
                User user = session.get(User.class, userId);
                
                if (user == null) {
                    transaction.rollback();
                    response.sendRedirect("login.html");
                    return;
                }
                
                // Check if user has enough leave balance
                boolean hasEnoughBalance = false;
                
                switch (leaveType) {
                    case "paid":
                        if (user.getPaidLeave() >= days) {
                            user.setPaidLeave(user.getPaidLeave() - days);
                            hasEnoughBalance = true;
                        }
                        break;
                    case "sick":
                        if (user.getSickLeave() >= days) {
                            user.setSickLeave(user.getSickLeave() - days);
                            hasEnoughBalance = true;
                        }
                        break;
                    case "casual":
                        if (user.getCasualLeave() >= days) {
                            user.setCasualLeave(user.getCasualLeave() - days);
                            hasEnoughBalance = true;
                        }
                        break;
                }
                
                if (!hasEnoughBalance) {
                    transaction.rollback();
                    response.sendRedirect("apply-leave.html?error=insufficient_balance");
                    return;
                }
                
                // Create new leave record
                Leave leave = new Leave();
                leave.setType(leaveType);
                leave.setDays(days);
                leave.setAppliedDate(LocalDate.now());
                leave.setUser(user);
                
                session.persist(leave);
                session.merge(user);
                transaction.commit();
                
                response.sendRedirect("home.html?success=leave_applied");
                
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("apply-leave.html?error=server");
        }
    }
}