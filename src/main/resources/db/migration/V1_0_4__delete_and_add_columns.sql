alter table users add column last_question varchar(1000);
alter table survey drop column last_question;