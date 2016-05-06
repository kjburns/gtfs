/*
 * TransferRule.java
 * General Transit Feed Specification
 * 
 * Copyright 2016 Kevin J. Burns
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Revision Log:
 *   2016-05-06  Basic functionality
 */
package com.github.kjburns.gtfs;

import com.github.kjburns.gtfs.misc.CsvFile;
import com.github.kjburns.gtfs.misc.CsvFile.FieldNotFoundException;
import com.github.kjburns.gtfs.misc.CsvFile.ReadPastEndOfTableException;

/**
 * A rule containing information about the ability to transfer between a
 * pair of stops. 
 * @author Kevin J. Burns
 *
 */
public class TransferRule {
	public enum TransferTypeEnum {
		/**
		 * From the spec:
		 * <blockquote>
		 * This is a recommended transfer point between two routes.
		 * </blockquote>
		 */
		RECOMMENDED_TRANSFER_POINT,
		/**
		 * From the spec:
		 * <blockquote>
		 * This is a timed transfer point between two routes. The departing 
		 * vehicle is expected to wait for the arriving one, with sufficient 
		 * time for a passenger to transfer between routes.
		 * </blockquote>
		 */
		TIMED_TRANSFER_POINT,
		/**
		 * From the spec:
		 * <blockquote>
		 * This transfer requires a minimum amount of time between arrival and 
		 * departure to ensure a connection. The time required to transfer is 
		 * specified by min_transfer_time.
		 * </blockquote>
		 */
		MINIMUM_TIME_REQUIRED_TRANSFER_POINT,
		/**
		 * From the spec:
		 * <blockquote>
		 * Transfers are not possible between routes at this location.
		 * </blockquote>
		 */
		NO_TRANSFER_POSSIBLE;
	}
	
	private static final String FIELD_NAME_FROM_STOP_ID = "from_stop_id";
	private static final String FIELD_NAME_TO_STOP_ID = "to_stop_id";
	private static final String FIELD_NAME_TRANSFER_TYPE = "transfer_type";
	private static final String FIELD_NAME_MIN_TRANSFER_TIME = 
			"min_transfer_time";
	
	private final String[] requiredFields = {
			TransferRule.FIELD_NAME_FROM_STOP_ID,
			TransferRule.FIELD_NAME_TO_STOP_ID, 
			TransferRule.FIELD_NAME_TRANSFER_TYPE 
	};
	
	private String fromStopId;
	private String toStopId;
	private int minTransferTime = -1;
	private TransferTypeEnum transferType;
	
	/**
	 * Reads a row from transfers.txt into a new object. There is no attempt
	 * to check whether the origin and destination stops actually exist.
	 * @param table table to read from
	 * @param record record to read from
	 * @throws MissingRequiredFieldException if a required field does not 
	 * exist in the table
	 * @throws InvalidDataException if either the minimum travel time or the
	 * transfer type do not fit with the specification's requirements
	 */
	TransferRule(CsvFile table, int record) 
			throws MissingRequiredFieldException, InvalidDataException {
		this.doRequiredFieldCheck(table);
		
		try {
			this.fromStopId = table.getData(FIELD_NAME_FROM_STOP_ID, record);

			this.toStopId = table.getData(FIELD_NAME_TO_STOP_ID, record);

			/*
			 * Parse transfer type
			 */
			String typeNumStr = table.getData(FIELD_NAME_TRANSFER_TYPE, record);
			InvalidDataException badTypeException = new InvalidDataException(
					GtfsFile.FILENAME_TRANSFERS, 
					FIELD_NAME_TRANSFER_TYPE, 
					record, 
					typeNumStr);
			int typeNum;
			if (typeNumStr.trim().equals("")) {
				typeNum = 0;
			}
			else {
				try {
					typeNum = Integer.valueOf(
							table.getData(FIELD_NAME_TRANSFER_TYPE, record));
				} catch (NumberFormatException ex) {
					throw badTypeException;
				}
			}
			
			if (	(typeNum < 0) || 
					(typeNum >= TransferTypeEnum.values().length)) {
				throw badTypeException;
			}
			else {
				this.transferType = TransferTypeEnum.values()[typeNum];
			}
			
			/*
			 * Parse transfer time if it exists
			 */
			if (table.fieldExists(FIELD_NAME_MIN_TRANSFER_TIME)) {
				String mttStr = table.getData(
						FIELD_NAME_MIN_TRANSFER_TIME, record);
				InvalidDataException badMttException =
						new InvalidDataException(
								GtfsFile.FILENAME_TRANSFERS, 
								FIELD_NAME_MIN_TRANSFER_TIME, 
								record, 
								mttStr);
				if (!mttStr.trim().equals("")) {
					/*
					 * If empty, don't try to parse
					 */
					try {
						int mtt = Integer.valueOf(mttStr);
						if (mtt < 0) {
							throw badMttException;
						}
						this.minTransferTime = mtt;
					} catch (NumberFormatException ex) {
						throw badMttException;
					} 
				}
			}
		} catch (ReadPastEndOfTableException ex) {
			/*
			 * Convert to unchecked
			 */
			throw new IndexOutOfBoundsException();
		} catch (FieldNotFoundException ex) {
			/*
			 * Has already been handled
			 */
			assert(false);
		}
	}
	
	private void doRequiredFieldCheck(CsvFile table) 
			throws MissingRequiredFieldException {
		for (int i = 0; i < this.requiredFields.length; i++) {
			if (!table.fieldExists(this.requiredFields[i])) {
				throw new MissingRequiredFieldException(
						GtfsFile.FILENAME_TRANSFERS, requiredFields[i]);
			}
		}
	}

	/**
	 * @return the stop id which is the origin for this transfer
	 */
	public String getFromStopId() {
		return this.fromStopId;
	}
	/**
	 * @return the stop id which is the destination for this transfer
	 */
	public String getToStopId() {
		return this.toStopId;
	}
	/**
	 * @return the minimum time required to make the transfer, in seconds; if
	 * it is not defined for this transfer rule, returns -1.
	 */
	public int getMinTransferTime() {
		return this.minTransferTime;
	}
	/**
	 * @return the type of transfer
	 */
	public TransferTypeEnum getTransferType() {
		return this.transferType;
	}
}
