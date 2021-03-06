package com.ten.aditum.statistics.service;

import com.ten.aditum.statistics.entity.PersonasLabel;
import com.ten.aditum.statistics.entity.PersonasPortrait;
import com.ten.aditum.statistics.util.TimeGenerator;
import com.ten.aditum.statistics.vo.Personas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * ServiceManager，不对应表，调用其他service进行服务
 */
@Slf4j
@Service
public class PersonasService {

    private static final int NO_DELETED = 0;
    private static final int IS_DELETED = 1;

    private final PersonService personService;
    private final PersonasLabelService personasLabelService;
    private final PersonasPortraitService personasPortraitService;

    @Autowired
    public PersonasService(PersonService personService, PersonasLabelService personasLabelService, PersonasPortraitService personasPortraitService) {
        this.personService = personService;
        this.personasLabelService = personasLabelService;
        this.personasPortraitService = personasPortraitService;
    }

    /**
     * 根据personId更新该用户的用户画像标签
     */
    public void updatePersonas(Personas personas) {
        if (personas.getPersonnelId() == null || personas.getLabelId() == null) {
            return;
        }

        // personas model array
        List<String> labelList = new ArrayList<>();
        boolean existed;
        int portraitId = 0;

        PersonasPortrait personasPortrait = selectPortrait(personas);
        // 当前用户画像不存在
        if (personasPortrait == null) {
            existed = false;
            labelList.add("新用户");
            labelList.add("青铜会员");
        }
        // 当前用户画像已存在
        else {
            existed = true;
            portraitId = personasPortrait.getId();
            String personasExt = personasPortrait.getPersonasExt();
            String[] personasList = personasExt.split(",");
            labelList.addAll(Arrays.asList(personasList));
        }

        // select model
        PersonasLabel personasLabel = new PersonasLabel()
                .setLabelId(personas.getLabelId())
                .setIsDeleted(NO_DELETED);

        List<PersonasLabel> personasLabelList = personasLabelService.select(personasLabel);
        if (personasLabelList.size() < 1) {
            log.warn("Personas [POST] Label FAILURE : {}", personas);
            return;
        }

        PersonasLabel selectLabel = personasLabelList.get(0);
        String labelName = selectLabel.getLabelName();

        // 未包含此标签
        if (!labelList.contains(labelName)) {
            labelList.add(labelName);
        }

        // 已存在，更新
        createOrUpdatePortrait(personas, labelList, existed, portraitId);
    }

    /**
     * 根据personId更新该用户的用户画像标签，可直接通过Name添加，而无需在Label表中有对应项
     */
    public void updatePersonasByLabelName(Personas personas) {
        if (personas.getPersonnelId() == null || personas.getLabelName() == null) {
            return;
        }

        // select portrait
        PersonasPortrait personasPortrait = new PersonasPortrait()
                .setPersonnelId(personas.getPersonnelId())
                .setIsDeleted(NO_DELETED);
        List<PersonasPortrait> personasPortraitList =
                personasPortraitService.select(personasPortrait);

        // personas model array
        List<String> labelList = new ArrayList<>();
        boolean existed;
        int portraitId = 0;

        // 当前用户画像不存在
        if (personasPortraitList.size() < 1) {
            existed = false;
            labelList.add("新用户");
            labelList.add("青铜会员");
        }
        // 当前用户画像已存在
        else {
            existed = true;
            PersonasPortrait select = personasPortraitList.get(0);
            portraitId = select.getId();
            String personasExt = select.getPersonasExt();
            String[] personasList = personasExt.split(",");
            labelList.addAll(Arrays.asList(personasList));
        }

        String labelName = personas.getLabelName();
        // 未包含此标签
        if (!labelList.contains(labelName)) {
            labelList.add(labelName);
        } else {
            log.info("用户 {} 已包含标签 {}", personas.getPersonnelId(), personas.getLabelName());
            return;
        }

        // 已存在，更新
        createOrUpdatePortrait(personas, labelList, existed, portraitId);
    }

    /**
     * 不存在则创建，存在则更新
     */
    private void createOrUpdatePortrait(Personas personas, List<String> labelList, boolean existed, int portraitId) {
        if (existed) {
            PersonasPortrait update = new PersonasPortrait()
                    .setId(portraitId)
                    .setPersonasExt(String.join(",", labelList))
                    .setUpdateTime(TimeGenerator.currentTime());
            personasPortraitService.update(update);
        }
        // 未存在，创建
        else {
            PersonasPortrait create = new PersonasPortrait()
                    .setPersonnelId(personas.getPersonnelId())
                    .setPersonasExt(String.join(",", labelList))
                    .setCreateTime(TimeGenerator.currentTime())
                    .setIsDeleted(NO_DELETED);
            personasPortraitService.insert(create);
        }
    }

    /**
     * 根据personId删除该用户的用户画像标签
     */
    public void removePersonas(Personas personas) {
        if (personas.getPersonnelId() == null || personas.getLabelId() == null) {
            return;
        }
        PersonasPortrait personasPortrait = selectPortrait(personas);
        if (personasPortrait == null) {
            return;
        }
        PersonasLabel personasLabel = selectLabel(personas);
        if (personasLabel == null) {
            return;
        }

        int portraitId = personasPortrait.getId();
        String personasExt = personasPortrait.getPersonasExt();
        String[] personasList = personasExt.split(",");
        List<String> labelList = new ArrayList<>(Arrays.asList(personasList));

        String labelName = personasLabel.getLabelName();

        // 若包含此标签，删除
        labelList.remove(labelName);

        PersonasPortrait update = new PersonasPortrait()
                .setId(portraitId)
                .setPersonasExt(String.join(",", labelList))
                .setUpdateTime(TimeGenerator.currentTime());
        personasPortraitService.update(update);
    }

    /**
     * 根据personId删除该用户的 模糊匹配 用户画像标签
     */
    public void removeFuzzyPersonasByKey(Personas personas) {
        if (personas.getPersonnelId() == null || personas.getLabelName() == null) {
            return;
        }

        PersonasPortrait personasPortrait = selectPortrait(personas);
        if (personasPortrait == null) {
            return;
        }

        String key = personas.getLabelName();

        int portraitId = personasPortrait.getId();
        String personasExt = personasPortrait.getPersonasExt();
        String[] personasList = personasExt.split(",");
        List<String> labelList = new ArrayList<>(Arrays.asList(personasList));

        // 若包含此关键字，删除
        Iterator iterator = labelList.iterator();
        while (iterator.hasNext()) {
            String label = (String) iterator.next();
            if (label.contains(key)) {
                iterator.remove();
            }
        }

        PersonasPortrait update = new PersonasPortrait()
                .setId(portraitId)
                .setPersonasExt(String.join(",", labelList))
                .setUpdateTime(TimeGenerator.currentTime());
        personasPortraitService.update(update);
    }

    // -------------------------------------------------------------- private

    /**
     * 根据personnelId查询PersonasPortrait
     */
    private PersonasPortrait selectPortrait(Personas personas) {
        PersonasPortrait personasPortrait = new PersonasPortrait()
                .setPersonnelId(personas.getPersonnelId())
                .setIsDeleted(NO_DELETED);
        List<PersonasPortrait> personasPortraitList = personasPortraitService.select(personasPortrait);
        if (personasPortraitList.size() < 1) {
            log.warn("当前用户画像不存在 : {}", personas.getLabelName());
            return null;
        }
        return personasPortraitList.get(0);
    }

    /**
     * 根据personnelId查询PersonasLabel
     */
    private PersonasLabel selectLabel(Personas personas) {
        PersonasLabel personasLabel = new PersonasLabel()
                .setLabelId(personas.getLabelId())
                .setIsDeleted(NO_DELETED);
        List<PersonasLabel> personasLabelList = personasLabelService.select(personasLabel);
        if (personasLabelList.size() < 1) {
            log.warn("Personas [POST] Label FAILURE : {}", personas);
            return null;
        }
        return personasLabelList.get(0);
    }

}
