package controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import model.User;
import util.HibernateUtil;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String department = request.getParameter("department");
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Check if email or username already exists
            Query<Long> emailQuery = session.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
            emailQuery.setParameter("email", email);
            
            Query<Long> usernameQuery = session.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
            usernameQuery.setParameter("username", username);
            
            if (emailQuery.uniqueResult() > 0) {
                response.sendRedirect("signup.html?error=email");
                return;
            }
            
            if (usernameQuery.uniqueResult() > 0) {
                response.sendRedirect("signup.html?error=username");
                return;
            }
            
            // Create new user
            User user = new User(name, email, username, password, department);
            
            Transaction transaction = session.beginTransaction();
            try {
                session.persist(user);
                transaction.commit();
                response.sendRedirect("login.html?success=registered");
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("signup.html?error=server");
        }
    }
}