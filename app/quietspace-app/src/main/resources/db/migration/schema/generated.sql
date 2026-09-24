
    create table chat (
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        id varchar(36) not null,
        created_by varchar(255),
        name varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table comment (
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        parent_id binary(16),
        id varchar(36) not null,
        post_id varchar(36) not null,
        user_id varchar(36) not null,
        text varchar(999) not null,
        created_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table message (
        is_seen bit not null,
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        photo_id binary(16),
        chat_id varchar(36),
        id varchar(36) not null,
        recipient_id varchar(36) not null,
        sender_id varchar(36) not null,
        text varchar(999) not null,
        created_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table notification (
        is_seen bit not null,
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        actor_id binary(16) not null,
        content_id binary(16) not null,
        user_id binary(16) not null,
        id varchar(36) not null,
        created_by varchar(255),
        updated_by varchar(255),
        content_type enum ('COMMENT','MESSAGE','POST','USER'),
        notification_type enum ('COMMENT','COMMENT_REACTION','COMMENT_REPLY','FOLLOW_REQUEST','MENTION','POST_REACTION','REPOST'),
        primary key (id)
    ) engine=InnoDB;

    create table photo (
        entity_type tinyint,
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        entity_id binary(16),
        user_id binary(16) not null,
        id varchar(36) not null,
        created_by varchar(255),
        name varchar(255),
        type varchar(255),
        updated_by varchar(255),
        data longblob,
        primary key (id)
    ) engine=InnoDB;

    create table poll (
        version integer not null,
        create_date datetime(6) not null,
        due_date datetime(6),
        update_date datetime(6),
        id varchar(36) not null,
        post_id varchar(36) not null,
        created_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table poll_option (
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        id varchar(36) not null,
        poll_id varchar(36) not null,
        label varchar(999) not null,
        created_by varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table poll_option_votes (
        votes binary(16),
        poll_option_id varchar(36) not null
    ) engine=InnoDB;

    create table post (
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        photo_id binary(16),
        id varchar(36) not null,
        user_id varchar(36) not null,
        text varchar(999),
        created_by varchar(255),
        repost_id varchar(255),
        repost_text varchar(255),
        title varchar(255),
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table profile_settings (
        is_allow_public_comments bit,
        is_allow_public_group_chat_invite bit,
        is_allow_public_message_requests bit,
        is_hide_like_counts bit,
        is_notifications_muted bit,
        is_private_account bit,
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        id varchar(36) not null,
        user_id varchar(36) not null,
        bio varchar(255),
        created_by varchar(255),
        updated_by varchar(255),
        blocked_users json,
        primary key (id)
    ) engine=InnoDB;

    create table reaction (
        version integer not null,
        create_date datetime(6) not null,
        update_date datetime(6),
        content_id binary(16) not null,
        user_id binary(16) not null,
        id varchar(36) not null,
        created_by varchar(255),
        updated_by varchar(255),
        username varchar(255) not null,
        content_type enum ('COMMENT','MESSAGE','POST','USER'),
        reaction_type enum ('DISLIKE','LIKE'),
        primary key (id)
    ) engine=InnoDB;

    create table token (
        version integer not null,
        create_date datetime(6) not null,
        expire_date datetime(6),
        update_date datetime(6),
        validate_date datetime(6),
        id varchar(36) not null,
        user_id varchar(36) not null,
        token varchar(600) not null,
        created_by varchar(255),
        email varchar(255) not null,
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table user (
        account_locked bit not null,
        enabled bit not null,
        role tinyint not null,
        status_type tinyint,
        version integer not null,
        create_date datetime(6) not null,
        date_of_birth datetime(6),
        update_date datetime(6),
        photo_id binary(16),
        email varchar(32) not null,
        username varchar(32) not null,
        id varchar(36) not null,
        created_by varchar(255),
        firstname varchar(255),
        lastname varchar(255),
        password varchar(255) not null,
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table user_chat (
        chat_id varchar(36) not null,
        user_id varchar(36) not null
    ) engine=InnoDB;

    create table user_followings (
        followings_id varchar(36) not null,
        user_id varchar(36) not null
    ) engine=InnoDB;

    create table user_saved_posts (
        post_id varchar(36) not null,
        user_id varchar(36) not null
    ) engine=InnoDB;

    alter table poll 
       add constraint UKp245s3319v7hiqj4ca6inlb9f unique (post_id);

    alter table poll_option_votes 
       add constraint UK1ndqfx2qol0cyfe4msf1rb7im unique (poll_option_id, votes);

    alter table profile_settings 
       add constraint UK1g81pq1rlegi5b2ege9blabwb unique (user_id);

    alter table token 
       add constraint UKpddrhgwxnms2aceeku9s2ewy5 unique (token);

    alter table user 
       add constraint UKob8kqyqqgmefl0aco34akdtpe unique (email);

    alter table user 
       add constraint UKsb8bbouer5wak8vyiiy4pf2bx unique (username);

    alter table comment 
       add constraint FKs1slvnkuemjsq2kj4h3vhx7i1 
       foreign key (post_id) 
       references post (id);

    alter table comment 
       add constraint FK8kcum44fvpupyw6f5baccx25c 
       foreign key (user_id) 
       references user (id);

    alter table message 
       add constraint FKmejd0ykokrbuekwwgd5a5xt8a 
       foreign key (chat_id) 
       references chat (id);

    alter table message 
       add constraint FKiup8wew331d92o7u3k8d918o3 
       foreign key (recipient_id) 
       references user (id);

    alter table message 
       add constraint FKcnj2qaf5yc36v2f90jw2ipl9b 
       foreign key (sender_id) 
       references user (id);

    alter table poll 
       add constraint FKj59dhk7k0402xqckwgwwp46tg 
       foreign key (post_id) 
       references post (id);

    alter table poll_option 
       add constraint FK81hniv4vvdii51krao5f84hen 
       foreign key (poll_id) 
       references poll (id);

    alter table poll_option_votes 
       add constraint FK6t2onukcbcqwf6o9trdwfqkwk 
       foreign key (poll_option_id) 
       references poll_option (id);

    alter table post 
       add constraint FK72mt33dhhs48hf9gcqrq4fxte 
       foreign key (user_id) 
       references user (id);

    alter table profile_settings 
       add constraint FK7mh0y9hqt7dalodhn1p6wvxpk 
       foreign key (user_id) 
       references user (id);

    alter table token 
       add constraint FKe32ek7ixanakfqsdaokm4q9y2 
       foreign key (user_id) 
       references user (id);

    alter table user_chat 
       add constraint FKfw0o0kaepbdntrg31qkbj3en7 
       foreign key (user_id) 
       references chat (id);

    alter table user_chat 
       add constraint FK60ku9ru56yadhj2tu35hshsms 
       foreign key (chat_id) 
       references user (id);

    alter table user_followings 
       add constraint FK1f1kxtjhmrvlvrhqmuwf9r7ls 
       foreign key (followings_id) 
       references user (id);

    alter table user_followings 
       add constraint FKj2a8435v8kbuogf5d8aaudfrp 
       foreign key (user_id) 
       references user (id);

    alter table user_saved_posts 
       add constraint FK7f91bvjqwx9ygykqxb84vcwm7 
       foreign key (post_id) 
       references post (id);

    alter table user_saved_posts 
       add constraint FKl10x36q7146xg8cvhl6dysi5r 
       foreign key (user_id) 
       references user (id);
