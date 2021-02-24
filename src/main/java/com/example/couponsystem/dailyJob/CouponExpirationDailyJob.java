package com.example.couponsystem.dailyJob;


import com.example.couponsystem.customExceptions.Logger;
import com.example.couponsystem.enums.eClientType;
import com.example.couponsystem.loginManager.LoginManager;
import com.example.couponsystem.services.AdminService;
import com.example.couponsystem.services.CompanyService;
import com.example.couponsystem.tables.Company;
import com.example.couponsystem.tables.Coupon;
import com.example.couponsystem.utiles.Singleton;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Component
public class CouponExpirationDailyJob implements CommandLineRunner
{
    private LoginManager loginManager;
    private AdminService adminService;
    private CompanyService companyService;
    private Logger logger = new Logger();


    public CouponExpirationDailyJob getInstance()
    {
        return Singleton.getInstance(CouponExpirationDailyJob.class);
    }


    public void removeAllExpiredCoupons()
    {
        loginManager = LoginManager.getInstance();
        adminService = (AdminService) loginManager.login("admin@admin.com", "admin", eClientType.Administrator);
        if(adminService == null)
        {
            return;
        }

        ArrayList<Company> companies = adminService.getAllCompanies();
        if(companies == null)
        {
            return;
        }

        for(Company company : companies)
        {
            companyService = (CompanyService) loginManager.login(company.getEmail(), company.getPassword(), eClientType.Company);
            if(companyService != null)
            {
                ArrayList<Coupon> companyCoupons = companyService.getCompanyCoupons();
                if(companyCoupons != null && !companyCoupons.isEmpty())
                {
                    for(Coupon coupon : companyCoupons)
                    {
                        if(coupon.isCouponExpired())
                        {
                            companyService.deleteCoupon(coupon.getId());
                            logger.log(String.format("Deleting Coupon %s because expired - > delete", coupon.toString()));
                        }
                    }
                }
            }
        }
    }


    Duration duration;
    @Override
    public void run(String... args) throws Exception
    {
//        TimerTask task = new TimerTask() {
//            public void run() {
//                removeAllExpiredCoupons();
//            }
//        };
//        Timer timer = new Timer("AllExpiredCouponsTimer");
//        timer.scheduleAtFixedRate(task, 0, 5000);
//

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(5).withMinute(0).withSecond(0);
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        duration = Duration.between(now, nextRun);
        long initalDelay = duration.getSeconds();

        TimerTask task = new TimerTask() {
            public void run() {
                removeAllExpiredCoupons();
            }
        };

        Timer timer = new Timer("AllExpiredCouponsTimer");
        timer.scheduleAtFixedRate(task, initalDelay, TimeUnit.DAYS.toSeconds(1));



    }
}