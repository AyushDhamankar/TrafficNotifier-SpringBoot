---

# 🚦 Traffic Alert System  

A **Spring Boot** application that monitors real-time traffic conditions and sends **automated email alerts** based on travel duration between a user's source and destination.  

## 🌟 Why I Built This Project  

As someone who commutes daily from **Virar to Thane Olympus**, my travel time usually ranges from **1.5 to 2 hours**. However, one evening, while returning home, I got caught in **unexpected heavy traffic** and was stuck for over **5 hours**, finally reaching home at **midnight**. With no prior warning, food, or water, the experience was exhausting and frustrating.  

That incident made me realize the need for a **smart alert system** that could notify me **before leaving my office** if there was heavy traffic on my route. This project was born out of that necessity—to help users plan their journeys better and avoid getting stuck in unexpected traffic delays. 🚦

## 🌟 Features  
- Users input **source, destination, expected arrival time, email, and mode of transport**.  
- At the selected time, the system fetches real-time **travel duration** using **Google Maps API**.  
- If the travel time **exceeds** the expected time, an **alert email** is sent indicating traffic.  
- If the travel time is **within limits**, a **good news email** is sent.  
- Uses **cron jobs (scheduler)** to check traffic at the specified time.  

## 🛠 Tech Stack  
- **Backend**: Java, Spring Boot  
- **Database**: MySQL  
- **Scheduling**: Cron Job Scheduler  
- **API Integration**: Google Maps API  
- **Email Service**: JavaMailSender  

## 📦 Setup & Installation  
1. Clone the repository:  
   ```bash
   git clone https://github.com/AyushDhamankar/TrafficNotifier-SpringBoot.git
   ```  
2. Import the project into your IDE (Eclipse/IntelliJ).  
3. Configure **MySQL Database** and update `application.properties`.  
4. Set up your **Google Maps API Key** in the project.  
5. Run the project on **Spring Boot**.

##
