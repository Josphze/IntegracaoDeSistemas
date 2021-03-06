package servlet;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import DTOs.GregorianCalendarDTO;
import DTOs.TripInfoDTO;
import DTOs.UserInfoDTO;
import beans.StatelessBean;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StatelessBean slb;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getParameter("createData") != null) {
            slb.createData();
            request.getRequestDispatcher("/secured/admin.jsp").forward(request, response);
        }

        else if (request.getParameter("eraseData") != null) {
            slb.eraseAllData();
            request.getRequestDispatcher("/secured/admin.jsp").forward(request, response);
        }

        else if (request.getParameter("deleteP") != null) {

            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            slb.deletePassenger(userId);

            request.getSession().invalidate();
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }

        else if (request.getParameter("deleteM") != null) {

            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            slb.deleteManager(userId);

            request.getSession().invalidate();
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }

        else if (request.getParameter("changeInfo") != null) {

            String email = request.getParameter("email");
            String key = request.getParameter("key");
            String name = request.getParameter("name");
            String phone = request.getParameter("phone");

            if (email.equals(""))
                email = null;
            else
                request.getSession(true).setAttribute("email", email);

            if (key.equals(""))
                key = null;

            if (name.equals(""))
                name = null;
            else
                request.getSession(true).setAttribute("name", name);

            if (phone.equals(""))
                phone = null;
            else
                request.getSession(true).setAttribute("phone", phone);

            UserInfoDTO userInfo = new UserInfoDTO(email, name, phone, hashPassword(key));
            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            slb.editPassenger(userId, userInfo);

            request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);
        }

        else if (request.getParameter("tripsBetweenDates") != null) {

            String s = request.getParameter("startDate");
            String st = request.getParameter("startTime");
            String e = request.getParameter("endDate");
            String et = request.getParameter("endTime");

            if (!s.equals("") && !e.equals("") && !st.equals("") && !et.equals("")) {

                String[] startDate = s.split("-");
                String[] endDate = e.split("-");
                String[] startTime = st.split(":");
                String[] endTime = et.split(":");

                GregorianCalendarDTO startCal = new GregorianCalendarDTO(Integer.parseInt(startDate[0]),
                        Integer.parseInt(startDate[1]) - 1, Integer.parseInt(startDate[2]),
                        Integer.parseInt(startTime[0]), Integer.parseInt(startTime[1]));
                GregorianCalendarDTO endCal = new GregorianCalendarDTO(Integer.parseInt(endDate[0]),
                        Integer.parseInt(endDate[1]) - 1, Integer.parseInt(endDate[2]), Integer.parseInt(endTime[0]),
                        Integer.parseInt(endTime[1]));

                List<TripInfoDTO> trips = slb.listFutureTripInfoBetweenStartEndDate(startCal, endCal);

                request.getSession(true).setAttribute("searchTrips", trips);
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid search, make sure you have selected both dates.");

            request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);
        }

        else if (request.getParameter("charge") != null) {

            double amount = Double.parseDouble(request.getParameter("chargeAmount"));

            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            slb.chargeWallet(userId, amount);

            // Refresh local display data
            UserInfoDTO uInfo = slb.getPassengerInfoById(userId);
            request.getSession(true).setAttribute("balance", uInfo.getBalance());

            request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);
        }

        else if (request.getParameter("purchase") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("tripToBuy"));
            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            if (slb.purchaseTicket(userId, tripId) == 0) {

                UserInfoDTO uInfo = slb.getPassengerInfoById(userId);
                request.getSession(true).setAttribute("balance", uInfo.getBalance());

                List<TripInfoDTO> myTrips = slb.listTripsByPassengerId(userId);
                request.getSession(true).setAttribute("myTrips", myTrips);
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid purchase, make sure you have enough money, that you're purchasing a future trip's ticket and that the trip has available capacity.");

            request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);
        }

        else if (request.getParameter("tripToRefund") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("tripToRefund"));

            Integer userId = (Integer) request.getSession(false).getAttribute("uId");

            if (slb.refundTicket(userId, slb.getTicketFromTrip(userId, tripId)) == 0) {

                // Update balance and trips
                UserInfoDTO uInfo = slb.getPassengerInfoById(userId);
                request.getSession(true).setAttribute("balance", uInfo.getBalance());

                List<TripInfoDTO> myTrips = slb.listTripsByPassengerId(userId);
                request.getSession(true).setAttribute("myTrips", myTrips);
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid refund, make sure you are refunding a future trip's ticket.");

            request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);
        }

        else if (request.getParameter("createTrip") != null) {

            String departureDate = request.getParameter("departureDate");
            String departureTime = request.getParameter("departureTime");
            String departurePoint = request.getParameter("departurePoint");
            String destination = request.getParameter("destination");
            String capacity = request.getParameter("capacity");
            String ticketPrice = request.getParameter("price");

            if (!departureDate.equals("") && !departurePoint.equals("") && !destination.equals("")
                    && !capacity.equals("") && !ticketPrice.equals("") && !departureTime.equals("")) {

                String[] d = departureDate.split("-");
                String[] t = departureTime.split(":");

                GregorianCalendarDTO dateDTO = new GregorianCalendarDTO(Integer.parseInt(d[0]),
                        Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]), Integer.parseInt(t[0]),
                        Integer.parseInt(t[1]));

                TripInfoDTO newTrip = new TripInfoDTO(dateDTO, departurePoint, destination, Integer.parseInt(capacity),
                        Double.parseDouble(ticketPrice));

                if (slb.addTrip(newTrip) == 1)
                    request.getSession().setAttribute("errorMessage",
                            "Invalid trip, make sure you filled in all fields correctly.");
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid trip, make sure you filled in all fields correctly.");

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("tripsBetweenDatesM") != null) {

            String s = request.getParameter("startDate");
            String st = request.getParameter("startTime");
            String e = request.getParameter("endDate");
            String et = request.getParameter("endTime");

            if (!s.equals("") && !e.equals("") && !st.equals("") && !et.equals("")) {

                String[] startDate = s.split("-");
                String[] endDate = e.split("-");
                String[] startTime = st.split(":");
                String[] endTime = et.split(":");

                GregorianCalendarDTO startCal = new GregorianCalendarDTO(Integer.parseInt(startDate[0]),
                        Integer.parseInt(startDate[1]) - 1, Integer.parseInt(startDate[2]),
                        Integer.parseInt(startTime[0]), Integer.parseInt(startTime[1]));
                GregorianCalendarDTO endCal = new GregorianCalendarDTO(Integer.parseInt(endDate[0]),
                        Integer.parseInt(endDate[1]) - 1, Integer.parseInt(endDate[2]), Integer.parseInt(endTime[0]),
                        Integer.parseInt(endTime[1]));

                List<TripInfoDTO> trips = slb.listTripInfoBetweenStartEndDate(startCal, endCal);

                request.getSession(true).setAttribute("searchTrips", trips);
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid search, make sure you have selected both dates.");

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("tripsSpecificDateM") != null) {

            String s = request.getParameter("date");

            if (!s.equals("")) {

                String[] startDate = s.split("-");

                GregorianCalendarDTO startCal = new GregorianCalendarDTO(Integer.parseInt(startDate[0]),
                        Integer.parseInt(startDate[1]) - 1, Integer.parseInt(startDate[2]));

                List<TripInfoDTO> trips = slb.listTripInfoByDepartureDate(startCal);

                request.getSession(true).setAttribute("specificSearchTrips", trips);
            }

            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid search, make sure you have selected a date.");

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("deleteTrip") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("tripToDelete"));

            if (slb.deleteTrip(tripId) == 0)
                request.getSession().removeAttribute("searchTrips");
            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid deletion, make sure you are deleting a future trip.");

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("deleteTripSpecific") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("tripToDelete"));

            if (slb.deleteTrip(tripId) == 0)
                request.getSession().removeAttribute("specificSearchTrips");
            else
                request.getSession().setAttribute("errorMessage",
                        "Invalid deletion, make sure you are deleting a future trip.");

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("listTop") != null) {

            List<UserInfoDTO> top = slb.listTop5Passengers();

            request.getSession().setAttribute("top", top);

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("showTripPassengers") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("trip"));

            List<UserInfoDTO> passengers = slb.listPassengersByTripId(tripId);

            request.getSession().setAttribute("passengers", passengers);

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }

        else if (request.getParameter("showTripPassengersSpecific") != null) {

            Integer tripId = Integer.parseInt(request.getParameter("trip"));

            List<UserInfoDTO> passengers = slb.listPassengersByTripId(tripId);

            request.getSession().setAttribute("passengersSpecific", passengers);

            request.getRequestDispatcher("/secured/manager.jsp").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getParameter("register") != null) {

            String email = request.getParameter("email");
            String key = request.getParameter("key");
            String name = request.getParameter("name");
            String phone = request.getParameter("phone");

            if (!email.equals("") && !key.equals("") && !name.equals("") && !phone.equals("")) {
                // Add passenger to database
                UserInfoDTO userInfo = new UserInfoDTO(email, name, phone, hashPassword(key));
                if (slb.addPassenger(userInfo) == 0) {

                    Integer uId = slb.getPassengerByEmail(email);

                    request.getSession(true).setAttribute("role", "passenger");
                    request.getSession(true).setAttribute("uId", uId);

                    // Set display variables
                    request.getSession(true).setAttribute("email", email);
                    request.getSession(true).setAttribute("name", name);
                    request.getSession(true).setAttribute("phone", phone);
                    request.getSession(true).setAttribute("balance", 0.0);
                    request.getRequestDispatcher("/secured/passenger.jsp").forward(request, response);

                } else {
                    request.getSession().setAttribute("errorMessage",
                            "Invalid register, make sure you aren't already registered.");
                    request.getRequestDispatcher("/index.jsp").forward(request, response);
                }
            } else {
                request.getSession().setAttribute("errorMessage",
                        "Invalid register, make sure you filled every parameter.");
                request.getRequestDispatcher("/index.jsp").forward(request, response);
            }

        }

        else if (request.getParameter("login") != null) {
            String email = request.getParameter("email");
            String key = request.getParameter("key");

            String destination = "/error.html";
            String hashedPw = hashPassword(key);

            // Credentials: admin@admin.com - admin
            if (email.equals("admin@admin.com") && hashedPw.equals("21232f297a57a5a743894a0e4a801fc3")) {
                request.getSession(true).setAttribute("role", "admin");
                destination = "/secured/admin.jsp";
            }

            else {
                String role = slb.authenticate(email, hashedPw);
                if (role.equals("passenger")) {

                    Integer uId = slb.getPassengerByEmail(email);

                    request.getSession(true).setAttribute("role", "passenger");
                    request.getSession(true).setAttribute("uId", uId);

                    // Get and set display variables from DTO
                    UserInfoDTO uInfo = slb.getPassengerInfoById(uId);
                    List<TripInfoDTO> myTrips = slb.listTripsByPassengerId(uId);

                    request.getSession(true).setAttribute("email", uInfo.getEmail());
                    request.getSession(true).setAttribute("name", uInfo.getName());
                    request.getSession(true).setAttribute("phone", uInfo.getPhoneNumber());
                    request.getSession(true).setAttribute("balance", uInfo.getBalance());

                    request.getSession(true).setAttribute("myTrips", myTrips);

                    destination = "/secured/passenger.jsp";
                }

                else if (role.equals("manager")) {
                    request.getSession(true).setAttribute("role", "manager");
                    request.getSession(true).setAttribute("uId", slb.getManagerByEmail(email));
                    destination = "/secured/manager.jsp";
                }

                else {
                    request.getSession(true).removeAttribute("role");
                    request.getSession(true).removeAttribute("uId");
                }
            }
            request.getRequestDispatcher(destination).forward(request, response);
        }

        else if (request.getParameter("logout") != null) {
            request.getSession().invalidate();
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }

    /*
     * Password hashing adapted from
     * https://howtodoinjava.com/java/java-security/how-to-generate-secure-password-
     * hash-md5-sha-pbkdf2-bcrypt-examples/
     */
    private String hashPassword(String password) {

        if (password == null)
            return null;

        try {
            // Create MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Add password bytes to digest
            md.update(password.getBytes());

            // Get the hash's bytes
            byte[] bytes = md.digest();

            // This bytes[] has bytes in decimal format. Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}