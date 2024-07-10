create table if not exists survey(
                                     id serial primary key,
chat_id bigint,
pain_date varchar(20),
    comment varchar(1000)
    );

create table if not exists tablets(

    id serial primary key,
    survey_id bigint references survey(id),
    name_tablets varchar(50),
    help boolean
);