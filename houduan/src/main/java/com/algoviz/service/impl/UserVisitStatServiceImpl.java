package com.algoviz.service.impl;

import com.algoviz.entity.UserVisitStat;
import com.algoviz.mapper.StatAccumulatorMapper;
import com.algoviz.mapper.UserVisitStatMapper;
import com.algoviz.service.UserVisitStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UserVisitStatServiceImpl implements UserVisitStatService {

    @Autowired
    private UserVisitStatMapper userVisitStatMapper;

    @Autowired
    private StatAccumulatorMapper statAccumulatorMapper;

    @Override
    @Transactional
    public boolean recordVisit(Long userId, String module) {
        if (module == null) {
            return false;
        }
        String m = module.toLowerCase();
        boolean valid;
        switch (m) {
            case "ds":   userVisitStatMapper.recordDsVisit(userId);   valid = true; break;
            case "algo": userVisitStatMapper.recordAlgoVisit(userId); valid = true; break;
            case "oj":   userVisitStatMapper.recordOjVisit(userId);   valid = true; break;
            case "ai":   userVisitStatMapper.recordAiVisit(userId);   valid = true; break;
            default:     valid = false; break;
        }
        if (valid) {
            // 写时累加：全量 stat_total +1（单行原子） 且 当日 stat_daily +1（同一事务，与 per-user 自增同生共死）
            Date today = Date.valueOf(LocalDate.now());
            switch (m) {
                case "ds":   statAccumulatorMapper.incTotalDs();  statAccumulatorMapper.incDailyDs(today);  break;
                case "algo": statAccumulatorMapper.incTotalAlgo(); statAccumulatorMapper.incDailyAlgo(today); break;
                case "oj":   statAccumulatorMapper.incTotalOj();  statAccumulatorMapper.incDailyOj(today);  break;
                case "ai":   statAccumulatorMapper.incTotalAi();  statAccumulatorMapper.incDailyAi(today);  break;
                default: break;
            }
        }
        return valid;
    }

    @Override
    public UserVisitStat getStats(Long userId) {
        UserVisitStat stat = userVisitStatMapper.findByUserId(userId);
        if (stat == null) {
            // 无行视为全 0，不落库（首次上报时 upsert 自动建行）
            stat = new UserVisitStat();
            stat.setUserId(userId);
            stat.setAiDialogues(0);
            stat.setDsVisits(0);
            stat.setAlgoVisits(0);
            stat.setOjVisits(0);
            stat.setUpdatedAt(LocalDateTime.now());
        }
        return stat;
    }

    @Override
    public void touchLogin(Long userId) {
        userVisitStatMapper.touchLogin(userId);
    }

    @Override
    public void initForUser(Long userId) {
        userVisitStatMapper.initForUser(userId);
    }
}
