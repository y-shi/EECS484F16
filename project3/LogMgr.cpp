#include "LogMgr.h"
#include <sstream>
#include <limits.h>
#include <map>


using namespace std;

///////////////////  LogMgr  ///////////////////
/*
class LogMgr {
 private:
  map <int, txTableEntry> tx_table;
  map <int, int> dirty_page_table;
  vector <LogRecord*> logtail;
*/
  /*
   * Find the LSN of the most recent log record for this TX.
   * If there is no previous log record for this TX, return
   * the null LSN.
   */
  int LogMgr::getLastLSN(int txnum){
	  if (this->tx_table.find(txnum) != this->tx_table.end())
		  return this->tx_table.find(txnum)->second.lastLSN;
	  else
		  return NULL_LSN;
  }

  /*
   * Update the TX table to reflect the LSN of the most recent
   * log entry for this transaction.
   */
  void LogMgr::setLastLSN(int txnum, int lsn){
	  if (this->tx_table.find(txnum) != this->tx_table.end())
		  this->tx_table.find(txnum)->second.lastLSN = lsn;
	  else
		  this->tx_table.insert(std::pair<int, txTableEntry>(txnum, *(new txTableEntry(lsn, U))));
	  /*
	  if (lsn == NULL_LSN)
		  this->tx_table.erase(txnum);
		  */
  }

  /*
   * Force log records up to and including the one with the
   * maxLSN to disk. Don't forget to remove them from the
   * logtail once they're written!
   */
  void LogMgr::flushLogTail(int maxLSN){
	  std::vector<LogRecord*>::iterator it = this->logtail.begin();
	  for (; it != this->logtail.end(); it++) {
		  if ((*it)->getLSN() > maxLSN)
			  break;
		  this->se->updateLog((*it)->toString());

	  }
	  this->logtail.erase(logtail.begin(), it);
  }

  //StorageEngine* se;

  /*
   * Run the analysis phase of ARIES.
   */
  void LogMgr::analyze(vector <LogRecord*> log){
	  int begin_ckpt = se->get_master(); //init = -1

	  for (std::vector<LogRecord*>::iterator it = log.begin(); it != log.end(); it++) {
		  if ((*it)->getLSN() < begin_ckpt)
			  continue;
		  if ((*it)->getType() == BEGIN_CKPT) {
			  ;
		  }
		  else if ((*it)->getType() == END_CKPT) {
			  this->tx_table = (dynamic_cast<ChkptLogRecord*> (*it))->getTxTable();
			  this->dirty_page_table = (dynamic_cast<ChkptLogRecord*> (*it))->getDirtyPageTable();
		  }
		  else if ((*it)->getType() == END) {
			  setLastLSN((*it)->getTxID(), NULL_LSN);
			  this->tx_table.erase((*it)->getTxID());
		  }
		  else {
			  setLastLSN((*it)->getTxID(), (*it)->getLSN());
			  if ((*it)->getType() == COMMIT) {
				  this->tx_table.find((*it)->getTxID())->second.status = C;
			  }
			  else {
				  //this->tx_table.find((*it)->getTxID())->second.status = U;
				  if ((*it)->getType() == CLR) {
					  if (this->dirty_page_table.find((dynamic_cast<CompensationLogRecord*> (*it))->getPageID()) == this->dirty_page_table.end())
						  this->dirty_page_table.insert(std::pair<int, int>((dynamic_cast<CompensationLogRecord*> (*it))->getPageID(), (dynamic_cast<CompensationLogRecord*> (*it))->getLSN()));
				  }
				  else if ((*it)->getType() == UPDATE) {
					  if (this->dirty_page_table.find((dynamic_cast<UpdateLogRecord*> (*it))->getPageID()) == this->dirty_page_table.end())
						  this->dirty_page_table.insert(std::pair<int, int>((dynamic_cast<UpdateLogRecord*> (*it))->getPageID(), (dynamic_cast<UpdateLogRecord*> (*it))->getLSN()));
				  }
			  }

		  }
	  }

  }

  /*
   * Run the redo phase of ARIES.
   * If the StorageEngine stops responding, return false.
   * Else when redo phase is complete, return true.
   */
  bool LogMgr::redo(vector <LogRecord*> log){

	  int begin = INT_MAX;
	  for (std::map <int, int>::iterator it = this->dirty_page_table.begin(); it != this->dirty_page_table.end(); it++) {
		  begin = std::min(begin, it->second);
	  }

	  for (std::vector<LogRecord*>::iterator it = log.begin(); it != log.end(); it++) {
		  int lsn = (*it)->getLSN();
		  if (lsn < begin)
			  continue;
		  if ((*it)->getType() == UPDATE) {
			  int pageID = (dynamic_cast<UpdateLogRecord*> (*it))->getPageID();
			  if (this->dirty_page_table.find(pageID) == this->dirty_page_table.end()) {
				  continue;
			  }
			  if (this->dirty_page_table.find(pageID)->second > lsn) {
				  continue;
			  }
			  if (se->getLSN(pageID) >= lsn) {
				  continue;
			  }
			  //redo
			  if (!(se->pageWrite(pageID, (dynamic_cast<UpdateLogRecord*> (*it))->getOffset(), (dynamic_cast<UpdateLogRecord*> (*it))->getAfterImage(), lsn))) {
				  return false;
			  }

		  }
		  else if ((*it)->getType() == CLR) {
			  int pageID = (dynamic_cast<CompensationLogRecord*> (*it))->getPageID();
			  if (this->dirty_page_table.find(pageID) == this->dirty_page_table.end()) {
				  continue;
			  }
			  if (this->dirty_page_table.find(pageID)->second > lsn) {
				  continue;
			  }
			  if (se->getLSN(pageID) >= lsn) {
				  continue;
			  }
			  //redo
			  if (!(se->pageWrite(pageID, (dynamic_cast<CompensationLogRecord*> (*it))->getOffset(), (dynamic_cast<CompensationLogRecord*> (*it))->getAfterImage(), lsn))) {
				  return false;
			  }

		  }
		  else if ((*it)->getType() == COMMIT) {
			  int txid = (*it)->getTxID();
			  if (this->tx_table.find(txid) != this->tx_table.end()) {
				  this->logtail.push_back(new LogRecord(se->nextLSN(), getLastLSN(txid), txid, END));
				  this->tx_table.erase(txid);
			  }

		  }

	  }
	  /*/end record
	  for (std::map <int, txTableEntry>::iterator it = this->tx_table.begin(); it != this->tx_table.end(); ) {
		  if (it->second.status == C) {
			  int txid = (*it).first;
			  this->logtail.push_back(new LogRecord(se->nextLSN(), getLastLSN(txid), txid, END));
			  //why error?? erase while iterating
			  //this->tx_table.erase(txid);
			  if (this->tx_table.find(txid) != this->tx_table.end())
				  this->tx_table.erase(it++);

		  }
		  else {
			  it++;
		  }
	  }
*/
	  return true;
  }

  /*
   * If no txnum is specified, run the undo phase of ARIES.
   * If a txnum is provided, abort that transaction.
   * Hint: the logic is very similar for these two tasks!
   */
  void LogMgr::undo(vector <LogRecord*> log, int txnum /*= NULL_TX*/){

	  //build toundo set
	  map<int, int> toUndo = *(new map<int, int>());
	  if (txnum != NULL_TX) {
		  toUndo.insert(std::pair<int, int>(txnum, NULL_LSN));
	  }
	  else {
		  for (std::map <int, txTableEntry>::iterator it = this->tx_table.begin(); it != this->tx_table.end(); it++) {
			  if (it->second.status == U) {
				  toUndo.insert(std::pair<int, int>(it->first, NULL_LSN));
			  }
		  }
	  }

	  //process log
	 for (std::vector<LogRecord*>::reverse_iterator rit = log.rbegin(); rit != log.rend(); rit++) {
		 if (toUndo.empty()) {
			 break;
		 }
		 int txid = (*rit)->getTxID();
		 if (toUndo.find(txid) == toUndo.end()) {
			 continue;
		 }
		 if ((*rit)->getType() == CLR) {
			 if (toUndo.find(txid)->second != NULL_LSN)
				 continue;
			 int next = (dynamic_cast<CompensationLogRecord*> (*rit))->getUndoNextLSN();
			 if (next != NULL_LSN) {
				 toUndo.find(txid)->second = next;
			 }
			 else {
				 //end
				 this->logtail.push_back(new LogRecord(se->nextLSN(), getLastLSN(txid), txid, END));
				 setLastLSN(txid, NULL_LSN);
				 //error?
				 rit++;
				 this->tx_table.erase(txid);
				 rit--;
				 toUndo.erase(txid);
			 }
		 }
		 else if ((*rit)->getType() == UPDATE) {
			 if (toUndo.find(txid)->second != NULL_LSN && toUndo.find(txid)->second != (*rit)->getLSN()) {
				 continue;
			 }
			 int lsn = se->nextLSN();
			 int pageID = (dynamic_cast<UpdateLogRecord*> (*rit))->getPageID();
			 int offset = (dynamic_cast<UpdateLogRecord*> (*rit))->getOffset();
			 string after_img = (dynamic_cast<UpdateLogRecord*> (*rit))->getBeforeImage();
			 int undo_next_lsn = (dynamic_cast<UpdateLogRecord*> (*rit))->getprevLSN();
			 toUndo.find(txid)->second = undo_next_lsn;
			 this->logtail.push_back(new CompensationLogRecord(lsn, getLastLSN(txid), txid,
					 pageID, offset,
				       after_img, undo_next_lsn));
			 setLastLSN(txid, lsn);

			 if (this->dirty_page_table.find(pageID) == this->dirty_page_table.end())
				 this->dirty_page_table.insert(std::pair<int, int>(pageID, lsn));
			 if(!(se->pageWrite(pageID, offset, after_img, lsn)))
				 return;
			 if (undo_next_lsn == NULL_LSN) {
				 //end
				 this->logtail.push_back(new LogRecord(se->nextLSN(), getLastLSN(txid), txid, END));
				 setLastLSN(txid, NULL_LSN);
				 //error?
				 rit++;
				 this->tx_table.erase(txid);
				 rit--;
				 toUndo.erase(txid);
			 }
		 }
	 }
  }

  vector<LogRecord*> LogMgr::stringToLRVector(string logstring){
	  vector<LogRecord*> result;
	  istringstream stream(logstring);
	  string line;
	  while(getline(stream, line)) {
	  LogRecord* lr = LogRecord::stringToRecordPtr(line);
	  result.push_back(lr);
	  }
	  return result;
  }

 //public:
  /*
   * Abort the specified transaction.
   * Hint: you can use your undo function
   */
  void LogMgr::abort(int txid){
	  int lsn = se->nextLSN();
	  this->logtail.push_back(new LogRecord(lsn, getLastLSN(txid), txid, ABORT));
	  setLastLSN(txid, lsn);
	  undo(this->logtail, txid);
	  undo(stringToLRVector(se->getLog()), txid);
	  //end
  }

  /*
   * Write the begin checkpoint and end checkpoint
   */
  void LogMgr::checkpoint(){
	  int begin = se->nextLSN();
	  this->logtail.push_back(new LogRecord(begin, NULL_LSN, NULL_TX, BEGIN_CKPT));
	  int end = se->nextLSN();
	  this->logtail.push_back(new ChkptLogRecord(end, begin, NULL_TX,
  		      this->tx_table,
  		      this->dirty_page_table));
	  flushLogTail(end);
	  se->store_master(begin);
  }

  /*
   * Commit the specified transaction.
   */
  void LogMgr::commit(int txid){
	  int lsn = se->nextLSN();
	  this->logtail.push_back(new LogRecord(lsn, getLastLSN(txid), txid, COMMIT));
	  flushLogTail(INT_MAX);

	  setLastLSN(txid, lsn);
	  this->tx_table.find(txid)->second.status = C;

	  lsn = se->nextLSN();
	  this->logtail.push_back(new LogRecord(lsn, getLastLSN(txid), txid, END));
	  setLastLSN(txid, NULL_LSN);
	  this->tx_table.erase(txid);
  }

  /*
   * A function that StorageEngine will call when it's about to
   * write a page to disk.
   * Remember, you need to implement write-ahead logging
   */
  void LogMgr::pageFlushed(int page_id){
	  if (this->dirty_page_table.find(page_id) != this->dirty_page_table.end()) {
		  std::vector<LogRecord*>::reverse_iterator rit = this->logtail.rbegin();
		  for (; rit != this->logtail.rend(); rit++) {
			  if ((*rit)->getType() == UPDATE) {
				  int pageID = (dynamic_cast<UpdateLogRecord*> (*rit))->getPageID();
				  if (pageID == page_id)
					  break;
			  }
			  else if ((*rit)->getType() == CLR) {
				  int pageID = (dynamic_cast<CompensationLogRecord*> (*rit))->getPageID();
				  if (pageID == page_id)
					  break;
			  }
		  }
		  if (rit != this->logtail.rend()) {
			  int lsn = (*rit)->getLSN();
			  flushLogTail(lsn);
		  }
		  this->dirty_page_table.erase(page_id);
	  }
	  //updated 1112, flush relative log record only.
  }

  /*
   * Recover from a crash, given the log from the disk.
   */
  void LogMgr::recover(string log){
	  vector<LogRecord*> v = stringToLRVector(log);
	  analyze(v);
	  if (redo(v))
		  undo(v);
  }

  /*
   * Logs an update to the database and updates tables if needed.
   */
  int LogMgr::write(int txid, int page_id, int offset, string input, string oldtext){
	  int lsn = se->nextLSN();
	  //se->write(txid, page_id, offset, input);
	  this->logtail.push_back(new UpdateLogRecord(lsn, getLastLSN(txid), txid,
				 page_id, offset,
				 oldtext, input));
	  setLastLSN(txid, lsn);

	  if (this->dirty_page_table.find(page_id) == this->dirty_page_table.end())
		  this->dirty_page_table.insert(std::pair<int, int>(page_id, lsn));

	  return lsn;
  }

  /*
   * Sets this.se to engine.
   */
  void LogMgr::setStorageEngine(StorageEngine* engine){
	  this->se = engine;
  }
/*
  //destructor
  ~LogMgr() {
    while (!logtail.empty()) {
      delete logtail[0];
      logtail.erase(logtail.begin());
    }
  }
  //copy constructor omitted
  //Overloaded assignment operator
  LogMgr &operator= (const LogMgr &rhs) {
    if (this == &rhs) return *this;
    //delete anything in the logtail vector
    while (!logtail.empty()) {
      delete logtail[0];
      logtail.erase(logtail.begin());
    }
    for (vector<LogRecord*>::const_iterator it = rhs.logtail.begin(); it !=rhs.logtail.end(); ++it) {
      LogRecord * lr = *it;
      int lsn = lr->getLSN();
      int prevLSN = lr->getprevLSN();
      int txid = lr->getTxID();
      TxType type = lr->getType();
      if (type == UPDATE) {
	UpdateLogRecord* ulr = dynamic_cast<UpdateLogRecord *>(lr);
	int page_id = ulr->getPageID();
	int offset  = ulr->getOffset();
	string before = ulr->getBeforeImage();
	string after = ulr->getAfterImage();
	UpdateLogRecord* cpy_lr = new UpdateLogRecord(lsn, prevLSN, txid, page_id, offset,
						      before, after);
	logtail.push_back(cpy_lr);
      } else if (type == CLR) {
	CompensationLogRecord* clr = dynamic_cast<CompensationLogRecord *>(lr);
	int page_id = clr->getPageID();
	int offset  = clr->getOffset();
	string after = clr->getAfterImage();
	int nextLSN = clr->getUndoNextLSN();
	CompensationLogRecord* cpy_lr = new CompensationLogRecord(lsn, prevLSN, txid, page_id, offset,
								  after, nextLSN);
	logtail.push_back(cpy_lr);
      } else if (type == END_CKPT) {
	ChkptLogRecord * chk_ptr = dynamic_cast<ChkptLogRecord *>(lr);
	map <int, txTableEntry> tx_table = chk_ptr->getTxTable();
	map <int, int> dp_table = chk_ptr->getDirtyPageTable();
	ChkptLogRecord * cpy_lr = new ChkptLogRecord(lsn, prevLSN, txid, tx_table, dp_table);
	logtail.push_back(cpy_lr);
      } else { //type is ordinary log record
	LogRecord * cpy_lr = new LogRecord(lsn, prevLSN, txid, type);
	logtail.push_back(cpy_lr);
      }
    }
    se = rhs.se;
    tx_table = rhs.tx_table;
    dirty_page_table = rhs.dirty_page_table;
    return *this;

  }
*/
