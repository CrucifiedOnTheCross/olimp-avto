package ru.olimpavto.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.dto.LeadResponse;
import ru.olimpavto.leads.LeadEntity;
import ru.olimpavto.leads.LeadMapper;
import ru.olimpavto.leads.LeadRepository;
import ru.olimpavto.mail.MailService;

class LeadsControllerTest {

    private final RecordingMailService mailService = new RecordingMailService();
    private final FakeLeadRepository leadRepository = new FakeLeadRepository();
    private final LeadsController controller = new LeadsController(mailService, leadRepository, new LeadMapper());

    @BeforeEach
    void setUp() {
        mailService.leadRequest = null;
        leadRepository.saved.clear();
        leadRepository.nextId = 1L;
    }

    @Test
    void sendLeadSavesToDatabaseSendsMailAndReturnsAccepted() {
        ConsultationRequest request = new ConsultationRequest()
                .name("Иван")
                .phone("+7 999 111-22-33")
                .comment("Хочу расчет")
                .policyAccepted(true);

        ResponseEntity<FormResponse> response = controller.sendLead(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Заявка отправлена");
        assertThat(mailService.leadRequest).isSameAs(request);
        assertThat(leadRepository.saved).hasSize(1);
        assertThat(leadRepository.saved.getFirst().getName()).isEqualTo("Иван");
        assertThat(leadRepository.saved.getFirst().getPhone()).isEqualTo("+7 999 111-22-33");
        assertThat(leadRepository.saved.getFirst().getComment()).isEqualTo("Хочу расчет");
        assertThat(leadRepository.saved.getFirst().getPolicyAccepted()).isTrue();
        assertThat(leadRepository.saved.getFirst().getCreatedAt()).isNotNull();
    }

    @Test
    void sendLeadRejectsMissingPolicyConsent() {
        ConsultationRequest request = new ConsultationRequest()
                .name("Иван")
                .phone("+7 999 111-22-33")
                .policyAccepted(false);

        assertThatThrownBy(() -> controller.sendLead(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Необходимо согласие на обработку персональных данных")
                .extracting("fields")
                .satisfies(fields -> assertThat(fields)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("policyAccepted", "Подтвердите согласие"));

        assertThat(mailService.leadRequest).isNull();
        assertThat(leadRepository.saved).isEmpty();
    }

    @Test
    void listLeadsReturnsSavedLeadsNewestFirst() {
        LeadEntity older = lead("Иван", "+7 999 111-22-33", OffsetDateTime.now().minusDays(1));
        LeadEntity newer = lead("Анна", "+7 999 222-33-44", OffsetDateTime.now());
        leadRepository.save(older);
        leadRepository.save(newer);

        ResponseEntity<List<LeadResponse>> response = controller.listLeads();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(LeadResponse::getName)
                .containsExactly("Анна", "Иван");
    }

    private static class RecordingMailService extends MailService {

        private ConsultationRequest leadRequest;

        RecordingMailService() {
            super(null, null);
        }

        @Override
        public void sendLead(ConsultationRequest request) {
            this.leadRequest = request;
        }
    }

    private LeadEntity lead(String name, String phone, OffsetDateTime createdAt) {
        LeadEntity lead = new LeadEntity();
        lead.setName(name);
        lead.setPhone(phone);
        lead.setPolicyAccepted(true);
        lead.setCreatedAt(createdAt);
        return lead;
    }

    private static class FakeLeadRepository implements LeadRepository {

        private final List<LeadEntity> saved = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public List<LeadEntity> findAllByOrderByCreatedAtDesc() {
            return saved.stream()
                    .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                    .toList();
        }

        @Override
        public <S extends LeadEntity> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            saved.removeIf(existing -> existing.getId().equals(entity.getId()));
            saved.add(entity);
            return entity;
        }

        @Override
        public List<LeadEntity> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public Optional<LeadEntity> findById(Long id) {
            return saved.stream().filter(lead -> lead.getId().equals(id)).findFirst();
        }

        @Override
        public boolean existsById(Long id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return saved.size();
        }

        @Override
        public void deleteById(Long id) {
            saved.removeIf(lead -> lead.getId().equals(id));
        }

        @Override
        public void delete(LeadEntity entity) {
            saved.remove(entity);
        }

        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {
            ids.forEach(this::deleteById);
        }

        @Override
        public void deleteAll(Iterable<? extends LeadEntity> entities) {
            entities.forEach(this::delete);
        }

        @Override
        public void deleteAll() {
            saved.clear();
        }

        @Override
        public <S extends LeadEntity> List<S> saveAll(Iterable<S> entities) {
            List<S> result = new ArrayList<>();
            entities.forEach(entity -> result.add(save(entity)));
            return result;
        }

        @Override
        public List<LeadEntity> findAllById(Iterable<Long> ids) {
            List<Long> requested = new ArrayList<>();
            ids.forEach(requested::add);
            return saved.stream().filter(lead -> requested.contains(lead.getId())).toList();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends LeadEntity> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends LeadEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<LeadEntity> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {
            deleteAllById(ids);
        }

        @Override
        public void deleteAllInBatch() {
            deleteAll();
        }

        @Override
        public LeadEntity getOne(Long id) {
            return getReferenceById(id);
        }

        @Override
        public LeadEntity getById(Long id) {
            return getReferenceById(id);
        }

        @Override
        public LeadEntity getReferenceById(Long id) {
            return findById(id).orElseThrow();
        }

        @Override
        public <S extends LeadEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity> List<S> findAll(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity> List<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Sort sort
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity> org.springframework.data.domain.Page<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Pageable pageable
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity> long count(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity> boolean exists(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends LeadEntity, R> R findBy(
                org.springframework.data.domain.Example<S> example,
                java.util.function.Function<
                        org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>,
                        R
                        > queryFunction
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<LeadEntity> findAll(org.springframework.data.domain.Sort sort) {
            return List.copyOf(saved);
        }

        @Override
        public org.springframework.data.domain.Page<LeadEntity> findAll(
                org.springframework.data.domain.Pageable pageable
        ) {
            throw new UnsupportedOperationException();
        }
    }
}
