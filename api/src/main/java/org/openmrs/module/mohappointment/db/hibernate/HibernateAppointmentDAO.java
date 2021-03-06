/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.mohappointment.db.hibernate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.management.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohappointment.db.AppointmentDAO;
import org.openmrs.module.mohappointment.model.MoHAppointment;
import org.openmrs.module.mohappointment.model.AppointmentState;
import org.openmrs.module.mohappointment.model.ServiceProviders;
import org.openmrs.module.mohappointment.model.Services;
import org.openmrs.module.mohappointment.singletonpattern.AppointmentList;

/**
 * @author Kamonyo
 * 
 *         This is the Appointment Services working together with the Hibernate
 */

@SuppressWarnings("unchecked")
public class HibernateAppointmentDAO implements AppointmentDAO {

	private SessionFactory sessionFactory;

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * @param sessionFactory
	 *            the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#getAllAppointments()
	 */

	@Override
	public Collection<MoHAppointment> getAllAppointments() {

		Session session = sessionFactory.getCurrentSession();

		Collection<MoHAppointment> appointments = session.createCriteria(
				MoHAppointment.class).list();

		return appointments;
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#saveAppointment(org.openmrs.module.appointment.IAppointment)
	 */
	@Override
	public Integer lastAppointmentId() {
		Integer lastId = 0;

		Session session = sessionFactory.getCurrentSession();
		lastId = (Integer) session
				.createSQLQuery(
						"SELECT appointment_id FROM moh_appointment ORDER BY appointment_id DESC LIMIT 1;")
				.uniqueResult();

		return lastId;
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#saveAppointment(org.openmrs.module.appointment.IAppointment)
	 */
	@Override
	public void saveAppointment(MoHAppointment appointment) {
		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(appointment);
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#updateAppointment(org.openmrs.module.appointment.IAppointment)
	 */
	@Override
	public void updateAppointment(MoHAppointment appointment) {
		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(appointment);
	}

	@Override
	public void updateState(MoHAppointment appointment, Integer stateId) {
		Session session = sessionFactory.getCurrentSession();
		session.createSQLQuery(
				"UPDATE moh_appointment SET appointment_state_id = " + stateId
						+ " WHERE appointment_id = "
						+ appointment.getAppointmentId() + ";").executeUpdate();

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#cancelAppointment
	 * (org.openmrs.module.mohappointment.service.IAppointment)
	 */
	@Override
	public void cancelAppointment(MoHAppointment appointment) {

		Session session = sessionFactory.getCurrentSession();
		appointment.setVoided(true);
		session.update(appointment);
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#getAppointmentById(int)
	 */

	@Override
	public MoHAppointment getAppointmentById(int appointmentId) {

		Session session = sessionFactory.getCurrentSession();

		MoHAppointment appointment = (MoHAppointment) session.load(MoHAppointment.class,
				appointmentId);

		return appointment;
	}

	/**
	 * @see org.openmrs.module.appointment.db.AppointmentDAO#getAppointmentsByMulti(java.lang.Object[])
	 */

	@Override
	public List<Integer> getAppointmentIdsByMulti(Object[] conditions, int limit) {

		Session session = sessionFactory.getCurrentSession();
		List<Integer> appointmentIds;

		StringBuilder combinedSearch = new StringBuilder("");

		if (conditions != null) {// Returns combined conditions matching
			// appointments.

			combinedSearch
					.append("SELECT DISTINCT appointment_id FROM moh_appointment WHERE ");

			if (null != conditions[0] && !conditions[0].equals(""))
				combinedSearch.append(" patient_id = " + conditions[0]
						+ " AND ");
			if (null != conditions[1])

				if (Context.getUserService().getUser(
						Integer.valueOf("" + conditions[1])) != null)
					if (!conditions[1].equals("")
							&& Context
									.getUserService()
									.getUser(
											Integer.valueOf("" + conditions[1]))
									.getPerson() != null)
						// Code has to be inserted here in order to load those
						// appointment depending on the provider working in this
						// service
						combinedSearch.append(" provider_id = "
								+ Context
										.getUserService()
										.getUser(
												Integer.valueOf(""
														+ conditions[1]))
										.getPerson().getPersonId() + " AND ");

			if (null != conditions[2] && !conditions[2].equals(""))
				combinedSearch.append(" location_id = " + conditions[2] + ""
						+ " AND ");
			if (null != conditions[3] && !conditions[3].equals("")) {
				combinedSearch.append(" appointment_date >= '"
						+ new SimpleDateFormat("yyyy-MM-dd")
								.format((Date) conditions[3]) + "' AND ");
			}
			if (null != conditions[4] && !conditions[4].equals("")) {
				if ((Boolean) conditions[4] == false)
					combinedSearch.append(" attended = "
							+ (Boolean) conditions[4] + " AND ");
				else
					// Here we need both Attended and Non-Attended when Include
					// Attended is selected
					combinedSearch
							.append(" (attended = TRUE OR attended = FALSE) AND ");
			}
			if (null != conditions[5] && !conditions[5].equals(""))
				combinedSearch.append(" appointment_date <= '"
						+ new SimpleDateFormat("yyyy-MM-dd")
								.format((Date) conditions[5]) + "' AND ");

			if (null != conditions[6] && !conditions[6].equals(""))
				combinedSearch.append(" appointment_state_id = "
						+ conditions[6] + " AND ");

			// Another condition object for services related to the
			// provider.
			if (null != conditions[7] && !conditions[7].equals(""))
				combinedSearch.append(" service_id = " + conditions[7]
						+ " AND ");

			// if (null != conditions[8] && !conditions[8].equals(""))
			// combinedSearch.append(" reason_obs_id = " + conditions[8] +
			// " AND ");

			combinedSearch
					.append(" voided = false ORDER BY appointment_date DESC LIMIT "
							+ limit + ";");

			appointmentIds = session.createSQLQuery(combinedSearch.toString())
					.list();

		} else {
			// Returns all future appointment not yet attended when nothing or
			// no conditions selected.
			appointmentIds = session
					.createSQLQuery(
							"SELECT appointment_id FROM moh_appointment WHERE attended = false AND voided = false AND appointment_date >= CURDATE() LIMIT 50")
					.list();
		}

		return appointmentIds;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#loadAllAppointments()
	 */
	@Override
	public void loadAllAppointments() {

		Session session = sessionFactory.getCurrentSession();
		Collection<MoHAppointment> appointments = session
				.createSQLQuery(
						"select app.* from moh_appointment app where voided = false;")
				.addEntity("app", MoHAppointment.class).list();

		if (appointments != null)
			if (appointments.size() == 0) {
				for (MoHAppointment appoint : appointments) {

					AppointmentList.getInstance().addAppointment(appoint);

				}
			}
	}

	// ************* AppointmentState DB Code *************

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#getAppointmentStates
	 * ()
	 */
	@Override
	public Collection<AppointmentState> getAppointmentStates() {
		Session session = getSessionFactory().getCurrentSession();
		List<AppointmentState> states = session.createCriteria(
				AppointmentState.class).list();
		return states;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @seeorg.openmrs.module.mohappointment.db.AppointmentDAO#
	 * getAppointmentStatesByName(java.lang.String)
	 */
	@Override
	public AppointmentState getAppointmentStatesByName(String name) {
		Session session = getSessionFactory().getCurrentSession();

		Object[] appState = (Object[]) session.createSQLQuery(
				"SELECT appointment_state_id, description "
						+ "FROM moh_appointment_state WHERE description = '"
						+ name + "';").uniqueResult();

		AppointmentState appointmentState = new AppointmentState(
				(Integer) appState[0], (String) appState[1]);

		return appointmentState;
	}

	// ************* Services and ServiceProviders DB Code *************

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#saveService(org.openmrs
	 * .module.mohappointment.service.Services)
	 */
	@Override
	public void saveService(Services service) {
		Session session = sessionFactory.getCurrentSession();
		session.save(service);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#saveServiceProviders
	 * (org.openmrs.module.mohappointment.service.IServiceProviders)
	 */
	@Override
	public void saveServiceProviders(ServiceProviders serviceProvider) {
		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(serviceProvider);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#updateService(org
	 * .openmrs.module.mohappointment.service.Services)
	 */
	@Override
	public void updateService(Services service) {
		saveService(service);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#updateServiceProviders
	 * (org.openmrs.module.mohappointment.service.IServiceProviders)
	 */
	@Override
	public void updateServiceProviders(ServiceProviders serviceProvider) {
		saveServiceProviders(serviceProvider);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#getPersonsByService
	 * (org.openmrs.module.mohappointment.service.Services)
	 */
	@Override
	public Collection<Integer> getPersonsByService(Services service) {
		Session session = sessionFactory.getCurrentSession();

		Collection<Integer> providers = session
				.createSQLQuery(
						"SELECT provider FROM moh_appointment_service_providers WHERE voided = 0 AND service = "
								+ service.getServiceId()).list();

		return providers;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#getServiceByProvider
	 * (org.openmrs.Person)
	 */
	@Override
	public Services getServiceByProvider(Person provider) {
		Session session = sessionFactory.getCurrentSession();

		if (provider != null) {
			ServiceProviders sp = (ServiceProviders) session
					.createCriteria(ServiceProviders.class)
					.add(Restrictions.eq("voided", false))
					.add(Restrictions.eq("provider", provider)).uniqueResult();
			try {
				return sp.getService();
			} catch (NullPointerException npe) {
				return null;
			}
		} else
			return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#getServiceById(java
	 * .lang.Integer)
	 */
	@Override
	public Services getServiceById(Integer serviceId) {
		Session session = sessionFactory.getCurrentSession();

		Services services = (Services) session.load(Services.class, serviceId);

		return services;
	}

	@Override
	public ServiceProviders getServiceProviderById(int serviceProviderId) {
		Session session = sessionFactory.getCurrentSession();

		ServiceProviders serviceProvider = (ServiceProviders) session.load(
				ServiceProviders.class, serviceProviderId);

		return serviceProvider;
	}

	/**
	 * @see org.openmrs.module.mohappointment.db.AppointmentDAO#getServiceByConcept(org.openmrs.Concept)
	 */
	@Override
	public Services getServiceByConcept(Concept concept) {
		Session session = sessionFactory.getCurrentSession();

		Services service = (Services) session.createCriteria(Services.class)
				.add(Restrictions.eq("concept", concept)).uniqueResult();

		return service;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openmrs.module.mohappointment.db.AppointmentDAO#getServiceProviders()
	 */
	@Override
	public Collection<ServiceProviders> getServiceProviders() {
		Session session = sessionFactory.getCurrentSession();

		Collection<ServiceProviders> serviceProviders = session
				.createCriteria(ServiceProviders.class)
				.add(Restrictions.eq("voided", false)).list();

		return serviceProviders;
	}

	/**
	 * (non-Jsdoc)
	 * 
	 * @see org.openmrs.module.mohappointment.db.AppointmentDAO#getServicesByProvider(org.openmrs.Person)
	 */
	@Override
	public Collection<Services> getServicesByProvider(Person provider) {
		Session session = sessionFactory.getCurrentSession();
		List<Services> services = new ArrayList<Services>();

		if (provider != null) {
			log.info("");
			List<ServiceProviders> serviceProviders = session
					.createCriteria(ServiceProviders.class)
					.add(Restrictions.eq("voided", false))
					.add(Restrictions.eq("provider", provider)).list();

			if (serviceProviders != null)
				for (ServiceProviders sp : serviceProviders) {
					if (sp.getProvider().equals(provider)) {
						// Services service = sp.getService();
						// for (Services serv : services) {
						// if (!serv.equals(service))
						// services.add(service);
						services.add(sp.getService());// to be commented if
														// uncomment above!
						// }
					}
				}

			return services;
		} else
			return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.module.mohappointment.db.AppointmentDAO#getServices()
	 */
	@Override
	public Collection<Services> getServices() {
		Session session = sessionFactory.getCurrentSession();

		List<Services> services = session.createCriteria(Services.class).list();

		return services;
	}

	public Collection<Integer> getAppointmentPatientName(String nameToMatch) {

		Session session = sessionFactory.getCurrentSession();

		List<Integer> appointmentIds = session.createSQLQuery(
				"SELECT appointment_id FROM moh_appointment WHERE appointment_id = "
						+ nameToMatch
						+ " AND voided = FALSE AND appointment_state_id <> ")
				.list();

		return appointmentIds;
	}

	/**
	 * @throws ParseException
	 * @see org.openmrs.module.mohappointment.db.AppointmentDAO#getAllWaitingAppointmentsByPatient(org.openmrs.Patient,
	 *      org.openmrs.module.mohappointment.model.AppointmentState,
	 *      java.util.Date)
	 */
	@Override
	public Collection<MoHAppointment> getAllWaitingAppointmentsByPatient(
			Patient patient, AppointmentState state, Date appointmentDate)
			throws ParseException {

		SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd");
		// SimpleDateFormat javaFormat = new SimpleDateFormat("dd/MM/yyyy");
		Session session = sessionFactory.getCurrentSession();

		Collection<Object[]> appointmentObjects = session
				.createSQLQuery(
						"SELECT appointment_id,appointment_date,note,attended,"
								+ "reason_obs_id,visit_date_obs_id,location_id,provider_id,"
								+ "service_id,encounter_id,patient_id,appointment_state_id,"
								+ "voided,voided_date,void_reason,voided_by,creator,created_date"
								+ " FROM moh_appointment"
								+ " WHERE appointment_date = '"
								+ sqlFormat.format(appointmentDate)
								+ "' AND appointment_state_id = "
								+ state.getAppointmentStateId().intValue()
								+ " AND patient_id = "
								+ patient.getPatientId().intValue()
								+ " AND voided = 0 AND attended = 0;").list();

		/**
		 * 0-appointment_id,1-appointment_date,2-note,3-attended,4-reason_obs_id
		 * , 5-visit_date_obs_id,6-location_id,7-provider_id,8-service_id,9-
		 * encounter_id,
		 * 10-patient_id,11-appointment_state_id,12-voided,13-voided_date
		 * ,14-void_reason, 15-voided_by,16-creator,17-created_date
		 */
		Collection<MoHAppointment> appointments = new ArrayList<MoHAppointment>();

		for (Object[] obj : appointmentObjects) {

			MoHAppointment appointment = new MoHAppointment();

			appointment.setAppointmentId((Integer) obj[0]);
			appointment.setAppointmentDate(appointmentDate);
			appointment.setNote((String) obj[2]);
			appointment.setAttended(false);

			if (obj[4] != null)
				appointment.setReason(Context.getObsService().getObs(
						(Integer) obj[4]));

			if (obj[5] != null)
				appointment.setNextVisitDate(Context.getObsService().getObs(
						(Integer) obj[5]));

			if (obj[6] != null)
				appointment.setLocation(Context.getLocationService()
						.getLocation((Integer) obj[6]));

			if (obj[7] != null)
				appointment.setProvider(Context.getPersonService().getPerson(
						(Integer) obj[7]));
			
			if (obj[8] != null)
				appointment.setService(getServiceById((Integer) obj[8]));

			if (obj[9] != null)
				appointment.setEncounter(Context.getEncounterService()
						.getEncounter((Integer) obj[9]));

			appointment.setPatient(patient);
			appointment.setAppointmentState(state);
			appointment.setVoided(false);

			if (obj[16] != null) {

				appointment.setCreator(Context.getUserService().getUser(
						(Integer) obj[16]));
			}

			if (obj[17] != null) {

				Date createdDate = sqlFormat.parse(obj[17].toString());
				appointment.setCreatedDate(createdDate);
			}

			appointments.add(appointment);
		}

		return appointments;
	}
}
